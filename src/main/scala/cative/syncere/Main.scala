package cative.syncere

import java.nio.file.FileSystems
import java.nio.file.WatchService
import java.nio.file.{StandardWatchEventKinds => K}

import scala.jdk.CollectionConverters.*
import cats.Show
import cats.data.Validated.Invalid
import cats.data.Validated.Valid
import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.syntax.flatMap.*

import cative.syncere.engine.Engine
import cative.syncere.filesystem.Creation
import cative.syncere.filesystem.Deletion
import cative.syncere.filesystem.Event
import cative.syncere.filesystem.Modification
import cative.syncere.given
import cative.syncere.meta.*

object Main extends IOApp {
  val con = IO.consoleForIO

  def poll(ws: WatchService, cli: Cli, s3: S3)(previous: Intels): IO[Intels] =
    for {
      key <- IO(ws.take())
      events = key.pollEvents.asScala.toList
        .map(Event.decodeWatchEvent(Config.SyncPath))
      next <- events.foldLeft(IO.pure(previous)) {
        case (ioIntels, validatedEvent) =>
          validatedEvent match {
            case Invalid(err) => ioIntels <* con.errorln(err)
            case Valid(event) =>
              event match {
                case e @ (_: Creation | _: Modification) =>
                  for {
                    intel <- FileSystem.intelForEvent(e)
                    intels <- ioIntels
                  } yield Engine.updateLocal(intels, intel)
                case Deletion(key) =>
                  ioIntels <* con.errorln(s"Deletion of $key ignored.")
              }
          }
      }
      _ <- IO(key.reset())
      actions = Engine.actions(next)
      _ <- printTagged("actions", actions)
    } yield next

  def printShow[A: Show](a: A): IO[Unit] =
    con.println(a)

  def printTagged[A: Show](tag: String, a: A): IO[Unit] =
    con.println(s"--- $tag: ---") >> printShow(a)

  override def run(args: List[String]): IO[ExitCode] = {
    val s3 = S3()

    val res = for {
      cli <- Cli.parse(args).toIO

      remoteDb <- s3.fetchIntels
      localDb <- FileSystem.fetchIntels
      unified = Engine.unite(localDb, remoteDb)
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
      con.error(help).as(ExitCode.Error)
    }
  }
}
