package cative.syncere.filesystem

import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.WatchService
import java.nio.file.{StandardWatchEventKinds => K}

import scala.jdk.CollectionConverters.*
import cats.data.Validated
import cats.data.Validated.Invalid
import cats.data.Validated.Valid
import cats.effect.IO
import cats.effect.Resource
import cats.syntax.monadError.*
import cats.syntax.traverse.*

import cative.syncere.*

object Watcher {
  def stripInvalid(ves: List[Validated[WatcherError, Event]]): IO[List[Event]] =
    ves.flatTraverse {
      case Invalid(err) => errorln(err).as(List.empty)
      case Valid(event) => IO.pure(List(event))
    }

  def watch(path: Path): Resource[IO, Watcher] =
    Resource
      .fromAutoCloseable(IO(FileSystems.getDefault().newWatchService()))
      .evalMap { ws =>
        val watcher = new Watcher(ws, path)
        watcher.register
      }
}

class Watcher private (ws: WatchService, path: Path) {
  private def register: IO[Watcher] =
    IO(path.register(ws, K.ENTRY_CREATE, K.ENTRY_DELETE, K.ENTRY_MODIFY))
      .as(this)

  def take: IO[List[Validated[WatcherError, Event]]] =
    for {
      key <- IO(ws.take())
      events = key.pollEvents.asScala.toList.map(Event.decodeWatchEvent(path))
      _ <- IO(key.reset())
        .ensure(WatcherError(s"Cannot reset $key"))(identity)
    } yield events

}
