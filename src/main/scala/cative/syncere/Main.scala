package cative.syncere

import cats.data.Validated.Invalid
import cats.data.Validated.Valid
import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.syntax.flatMap.*

import cative.syncere.engine.Engine
import cative.syncere.engine.Intels
import cative.syncere.filesystem.Event
import cative.syncere.filesystem.Watcher
import cative.syncere.given

class Main(cli: Cli, s3: S3) {
  def poll(watcher: Watcher)(previous: Intels): IO[Intels] =
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
      actions = Engine.actions(next)
      _ <- printTagged("actions", actions)
      _ <- IO.whenA(cli.wetRun)(s3.playAll(actions))
    } yield next

  val run: IO[ExitCode] =
    for {
      remoteDb <- s3.fetchIntels
      localDb <- FileSystem.fetchIntels
      unified = Intels.fresh(localDb ++ remoteDb)
      actions = Engine.actions(unified)
      _ <- printTagged("unified state", unified)
      _ <- printTagged("actions", actions)

      _ <- IO.whenA(cli.wetRun)(s3.playAll(actions))
      _ <- IO.whenA(cli.isForever) {
        for {
          watcher <- Watcher.watch(Config.SyncPath)
          _ <- unified.iterateForeverM(poll(watcher))
        } yield ()
      }

    } yield ExitCode.Success
}

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    val res = for {
      cli <- Cli.parse(args).toIO
      exitCode <- S3().use(s3 => Main(cli, s3).run)
    } yield exitCode
    res.recoverWith { case CliError(help) =>
      error(help).as(ExitCode.Error)
    }
  }
}
