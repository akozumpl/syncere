package cative.syncere

import java.nio.file.FileSystems
import java.nio.file.WatchService
import java.nio.file.{StandardWatchEventKinds => K}

import scala.jdk.CollectionConverters.*
import cats.data.Validated.Invalid
import cats.data.Validated.Valid
import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.syntax.flatMap.*

import cative.syncere.engine.Engine
import cative.syncere.engine.Intels
import cative.syncere.filesystem.Event
import cative.syncere.given

object Main extends IOApp {
  def poll(ws: WatchService, cli: Cli, s3: S3)(previous: Intels): IO[Intels] =
    for {
      key <- IO(ws.take())
      events = key.pollEvents.asScala.toList
        .map(Event.decodeWatchEvent(Config.SyncPath))
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
      _ <- IO(key.reset())
      _ <- printTagged("next state", next)
      actions = Engine.actions(next)
      _ <- printTagged("actions", actions)
      _ <- IO.whenA(cli.wetRun)(s3.playAll(actions))
    } yield next

  override def run(args: List[String]): IO[ExitCode] = {
    val s3 = S3()

    val res = for {
      cli <- Cli.parse(args).toIO

      remoteDb <- s3.fetchIntels
      localDb <- FileSystem.fetchIntels
      unified = Intels.fresh(localDb ++ remoteDb)
      actions = Engine.actions(unified)
      _ <- printTagged("unified state", unified)
      _ <- printTagged("actions", actions)

      _ <- IO.whenA(cli.wetRun)(s3.playAll(actions))
      _ <- IO.whenA(cli.isForever) {
        for {
          ws <- IO(FileSystems.getDefault().newWatchService())
          _ <- IO(
            Config.SyncPath
              .register(ws, K.ENTRY_CREATE, K.ENTRY_DELETE, K.ENTRY_MODIFY)
          )
          _ <- unified.iterateForeverM(poll(ws, cli, s3))
        } yield ()
      }

    } yield ExitCode.Success
    res.recoverWith { case CliError(help) =>
      error(help).as(ExitCode.Error)
    }
  }
}
