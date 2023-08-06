package cative.syncere.engine

import cative.syncere.meta.KeyEntry
import cats.Show
import cats.syntax.show._

sealed trait Action

case class Download(k: KeyEntry) extends Action

case class Upload(k: KeyEntry) extends Action

object Action {
  given showInstance: Show[Action] = Show { action =>
    action match {
      case Upload(k) =>
        s"---> ${k.name}"
      case Download(k) =>
        s"<--- ${k.name}"
    }
  }

  given showListInstance: Show[List[Action]] = Show { actions =>
    "=== show Actions:\n" + actions.map(_.show).mkString("\n")
  }

}
