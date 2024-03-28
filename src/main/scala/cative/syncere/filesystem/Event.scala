package cative.syncere.filesystem

import java.nio.file.Path
import java.nio.file.WatchEvent

import cats.Show
import cats.data.Validated
import cats.syntax.either._

import cative.syncere.meta.KeyEntry.Key

sealed trait Event {
  def key: Key
  def path: Path
}

case class Creation(key: Key, path: Path) extends Event
case class Deletion(key: Key, path: Path) extends Event
case class Modification(key: Key, path: Path) extends Event

object Event {

  private def decodePath(
      event: WatchEvent[_]
  ): Either[WatcherError, Path] =
    event.context() match {
      case p: Path => p.asRight
      case _ =>
        WatcherError(
          s"Failed to decode path from event ${event.toString}."
        ).asLeft
    }

  given Show[Event] = Show(e => s"${e.getClass.getSimpleName()} ${e.key}")

  def decodeWatchEvent(root: Path)(
      watchEvent: WatchEvent[_]
  ): Validated[WatcherError, Event] = {
    val event = for {
      relativePath <- decodePath(watchEvent)
      key = relativePath.toString
      path = root.resolve(relativePath)
      event <- watchEvent.kind().name() match {
        case "ENTRY_CREATE" =>
          Creation(key, path).asRight
        case "ENTRY_DELETE" =>
          Deletion(key, path).asRight
        case "ENTRY_MODIFY" =>
          Modification(key, path).asRight
        case name =>
          WatcherError(
            s"Failed to decode event type for $path: $name."
          ).asLeft
      }
    } yield event
    event.toValidated
  }

}
