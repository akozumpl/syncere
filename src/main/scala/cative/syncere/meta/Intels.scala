package cative.syncere.meta

import cats.Show
import cats.syntax.show._

case class Intels(intels: Seq[Intel])

object Intels {
  given Show[Intels] = Show.show(
    _.intels.map(_.show).mkString("\n")
  )
}
