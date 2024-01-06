package cative.syncere.filesystem

import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.WatchService
import java.nio.file.{StandardWatchEventKinds => K}

import scala.jdk.CollectionConverters.*
import cats.data.Validated
import cats.effect.IO
import cats.syntax.monadError.*

object Watcher {
  def watch(path: Path): IO[Watcher] =
    for {
      ws <- IO(FileSystems.getDefault().newWatchService())
      watcher = new Watcher(ws, path)
      _ <- watcher.register(path)
    } yield watcher

}

class Watcher private (ws: WatchService, path: Path) {
  private def register(path: Path): IO[Unit] =
    IO(path.register(ws, K.ENTRY_CREATE, K.ENTRY_DELETE, K.ENTRY_MODIFY)).as(())

  def take: IO[List[Validated[WatchServiceError, Event]]] =
    for {
      key <- IO(ws.take())
      events = key.pollEvents.asScala.toList.map(Event.decodeWatchEvent(path))
      _ <- IO(key.reset())
        .ensure(WatchServiceError(s"Cannot reset $key"))(identity)
    } yield events

}
