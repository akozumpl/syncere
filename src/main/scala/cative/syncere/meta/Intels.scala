package cative.syncere.meta

import cats.Show
import cats.syntax.contravariant._
import cats.syntax.show._

import cative.syncere.given

case class Intels(intels: Seq[Intel])

object Intels {
  given Show[Intels] = Show[List[Intel]].contramap(_.intels.toList)
}
