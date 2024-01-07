package cative.syncere

import cats.data.Validated.Invalid
import cats.data.Validated.Valid
import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.effect.kernel.Resource
import cats.syntax.flatMap.*

import cative.syncere.engine.Engine
import cative.syncere.engine.Intels
import cative.syncere.filesystem.Event
import cative.syncere.filesystem.Watcher
import cative.syncere.given

class Main(cli: Cli, s3: S3, watcher: Watcher) {
  def play(intels: Intels): IO[Intels] = {
    val actions = Engine.actions(intels)
    for {
      _ <- printTagged("actions", actions)
      next <-
        if (cli.wetRun) s3.playAll(actions).as(intels.absorbAllActions(actions))
        else IO.pure(intels)
    } yield next
  }

  def poll(previous: Intels): IO[Intels] =
    for {
      events <- watcher.take
      next <- events.foldLeft(IO.pure(previous)) {
        case (ioIntels, validatedEvent) =>
          validatedEvent match {
            case Invalid(err) => ioIntels <* errorln(err)
            case Valid(event) =>
              for {
                intel <- FileSystem.intelForEvent(event)
                intels <- ioIntels
              } yield intels.absorb(intel)
          }
      }
      _ <- printTagged("next state", next)
      played <- play(next)
    } yield played

  val run: IO[ExitCode] =
    for {
      remoteDb <- s3.fetchIntels
      localDb <- FileSystem.fetchIntels
      unified = Intels.fresh(localDb ++ remoteDb)
      _ <- printTagged("fresh unified state", unified)
      intels <- play(unified)

      _ <- IO.whenA(cli.isForever)(
        unified.iterateForeverM(poll).as(())
      )

    } yield ExitCode.Success
}

object Main extends IOApp {

  def mainResource(cli: Cli): Resource[IO, Main] =
    for {
      s3 <- S3.apply
      watcher <- Watcher.watch(Config.SyncPath)
    } yield Main(cli, s3, watcher)

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
