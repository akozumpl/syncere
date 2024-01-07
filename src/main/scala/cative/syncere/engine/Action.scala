package cative.syncere.engine

import cats.Show
import cats.syntax.show.*

import cative.syncere.meta.KeyEntry
import cative.syncere.meta.Local
import cative.syncere.meta.Remote

sealed trait Action

case class DeleteRemotely(k: KeyEntry.Key) extends Action
case class Download(r: Remote) extends Action
case object NoOp extends Action
case class Upload(l: Local) extends Action

object Action {
  given showInstance: Show[Action] = Show { action =>
    action match {
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
