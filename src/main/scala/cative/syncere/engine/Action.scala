package cative.syncere.engine

import cats.Show
import cats.syntax.show._

import cative.syncere.meta.KeyEntry

sealed trait Action

case class Delete(k: KeyEntry.Key) extends Action
case class Download(k: KeyEntry.Key) extends Action
case object NoOp extends Action
case class Upload(k: KeyEntry.Key) extends Action

object Action {
  given showInstance: Show[Action] = Show { action =>
    action match {
      case Delete(k) =>
        s"---X $k"
      case Download(k) =>
        s"<--- $k"
      case NoOp =>
        "<noop>"
      case Upload(k) =>
        s"---> $k"
    }
  }

}
