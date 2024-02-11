package cative.syncere.engine

import java.time.Instant

import cats.Show
import cats.syntax.option.*

import cative.syncere.meta.*

/** Action to perform so that synchronicity increases.
  *
  * Most will have some form of a `result()` method which predicts the Intel
  * performing the action will produce. The intel will tend to have a stale
  * timestamp: this is OK, an actual event (on a remote refresh or a filesystem
  * event) is fine to override this one.
  */
sealed trait Action

case class DeleteLocally(k: KeyEntry.Key) extends Action {
  def result(seen: Instant) = LocallyDeleted(k, seen)
}

case class DeleteRemotely(k: KeyEntry.Key) extends Action {
  val result = RemotelyDeleted(k)
}

case class Download(r: Remote) extends Action {
  val result = Local(r.key, r.tag, r.lastChange)
}

case object NoOp extends Action

case class Upload(l: Local) extends Action {
  val result = Remote(l.key, l.tag, l.lastChange)
}

object Action {
  given showInstance: Show[Action] = Show { action =>
    action match {
      case DeleteLocally(k) =>
        s"X--- $k"
      case DeleteRemotely(k) =>
        s"---X $k"
      case Download(r) =>
        s"<--- ${r.key}"
      case NoOp =>
        "<noop>"
      case Upload(l) =>
        s"---> ${l.key}"
    }
  }
}
