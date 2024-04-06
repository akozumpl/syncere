package cative.syncere

import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.effect.kernel.Resource
import cats.effect.std.Queue
import cats.syntax.flatMap.*
import cats.syntax.traverse.*

import cative.syncere.engine.Engine
import cative.syncere.engine.Intels
import cative.syncere.filesystem.SyncDir
import cative.syncere.filesystem.Watcher
import cative.syncere.given
import cative.syncere.meta.FreshIntel

class Main(
    queue: Queue[IO, FreshIntel],
    cli: Cli,
    s3: S3,
    syncDir: SyncDir,
    watcher: Watcher
) {

  private def blockingDrain(intels: Intels): IO[Intels] =
    for {
      first <- queue.take
      rest <- queue.tryTakeN(None)
    } yield intels.absorbAll(first :: rest)

  private def queueAll(isIo: IO[List[FreshIntel]]): IO[Unit] =
    for {
      is <- isIo
      _ <- is.traverse(queue.offer)
    } yield ()

  def play(intels: Intels): IO[Intels] = {
    val actions = Engine.actions(intels)
    for {
      _ <- printTagged("actions", actions)
      next <-
        if (cli.wetRun) s3.playAll(actions).map(intels.absorbAll)
        else IO.pure(intels)
    } yield next
  }

  /** Polls S3 for one iteration. */
  def poll: IO[Unit] = for {
    _ <- IO.sleep(cli.pollInterval)
    _ <- printShow("Polling S3.")
    _ <- queueAll(s3.fetchIntels)
  } yield ()

  /** Watches the filesystem for one iteration. */
  def watch: IO[Unit] = for {
    validatedEvents <- watcher.take
    events <- Watcher.stripInvalid(validatedEvents)
    _ <- printTagged("polled events", events)
    intels = syncDir.intelsSansProblems(events)
    _ <- queueAll(intels)
  } yield ()

  def consumeQueue(previous: Intels): IO[Intels] =
    for {
      next <- blockingDrain(previous)
      played <-
        if (next == previous) IO.pure(previous)
        else printTagged("state", next) *> play(next)
    } yield played

  val run: IO[ExitCode] = for {
    _ <- queueAll(s3.fetchIntels)
    _ <- queueAll(syncDir.fetchIntels)

    _ <- IO.whenA(cli.isForever) {
      for {
        _ <- poll.foreverM.start
        _ <- watch.foreverM.start
        _ <- Intels.empty.iterateForeverM(consumeQueue)
      } yield ()
    }
  } yield ExitCode.Success
}

object Main extends IOApp {

  def mainResource(cli: Cli): Resource[IO, Main] = {
    val syncDir = SyncDir(cli.syncPath)
    for {
      s3 <- S3(cli.s3Bucket, syncDir, cli.awsConfigProfile)
      watcher <- Watcher.watch(cli.syncPath)
      queue <- Resource.eval(Queue.unbounded[IO, FreshIntel])
    } yield Main(queue, cli, s3, syncDir, watcher)
  }

  override def run(args: List[String]): IO[ExitCode] = {
    val res = for {
      cli <- Cli.parse(args).toIO
      exitCode <- mainResource(cli).use(_.run)
    } yield exitCode
    res.recoverWith { case CliError(help) =>
      error(help).as(ExitCode.Error)
    }
  }
}
