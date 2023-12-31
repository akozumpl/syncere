package cative.syncere.filesystem

import java.nio.file.Path
import java.nio.file.WatchEvent

import cats.data.Validated
import cats.syntax.either._

import cative.syncere.meta.KeyEntry.Key

sealed trait Event {
  def key: Key = path.toString
  def path: Path
}

case class Creation(path: Path) extends Event
case class Deletion(path: Path) extends Event
case class Modification(path: Path) extends Event

object Event {

  private def decodePath(
      event: WatchEvent[_]
  ): Either[WatchServiceError, Path] =
    event.context() match {
      case p: Path => p.asRight
      case _ =>
        WatchServiceError(
          s"Failed to decode path from event ${event.toString}."
        ).asLeft
    }

  def decodeWatchEvent(root: Path)(
      watchEvent: WatchEvent[_]
  ): Validated[WatchServiceError, Event] = {
    val event = for {
      relativeKey <- decodePath(watchEvent)
      key = root.resolve(relativeKey)
      event <- watchEvent.kind().name() match {
        case "ENTRY_CREATE" =>
          Creation(key).asRight
        case "ENTRY_DELETE" =>
          Deletion(key).asRight
        case "ENTRY_MODIFY" =>
          Modification(key).asRight
        case name =>
          WatchServiceError(
            s"Failed to decode event type for $key: $name."
          ).asLeft
      }
    } yield event
    event.toValidated
  }

}
