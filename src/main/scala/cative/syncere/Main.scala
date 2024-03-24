package cative.syncere

import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.effect.kernel.Resource
import cats.syntax.flatMap.*

import cative.syncere.engine.Engine
import cative.syncere.engine.Intels
import cative.syncere.filesystem.SyncDir
import cative.syncere.filesystem.Watcher
import cative.syncere.given

class Main(cli: Cli, s3: S3, syncDir: SyncDir, watcher: Watcher) {
  def play(intels: Intels): IO[Intels] = {
    val actions = Engine.actions(intels)
    for {
      _ <- printTagged("actions", actions)
      next <-
        if (cli.wetRun) s3.playAll(actions).map(intels.absorbAll)
        else IO.pure(intels)
    } yield next
  }

  def poll(previous: Intels): IO[Intels] =
    for {
      validatedEvents <- watcher.take
      events <- Watcher.stripInvalid(validatedEvents)
      _ <- printTagged("polled events", events)
      intels <- syncDir.intelsSansProblems(events)
      next = intels.foldLeft(previous) { case (intels, intel) =>
        previous.absorb(intel)
      }
      _ <- printTagged("next state", next)
      played <- play(next)
    } yield played

  val run: IO[ExitCode] =
    for {
      remoteDb <- s3.fetchIntels
      localDb <- syncDir.fetchIntels
      unified = Intels.fresh(localDb ++ remoteDb)
      _ <- printTagged("fresh unified state", unified)
      intels <- play(unified)

      _ <- IO.whenA(cli.isForever)(
        intels.iterateForeverM(poll).as(())
      )

    } yield ExitCode.Success
}

object Main extends IOApp {

  def mainResource(cli: Cli): Resource[IO, Main] = {
    val syncDir = SyncDir(cli.syncPath)
    for {
      s3 <- S3(cli.s3Bucket, syncDir, cli.awsConfigProfile)
      watcher <- Watcher.watch(cli.syncPath)
    } yield Main(cli, s3, syncDir, watcher)
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
