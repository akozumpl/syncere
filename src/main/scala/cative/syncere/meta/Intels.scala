package cative.syncere.meta

import cats.Show
import cats.syntax.contravariant._
import cats.syntax.show._

import cative.syncere.given
import cative.syncere.meta.KeyEntry.Key

case class Intels(intels: Map[Key, Intel])

object Intels {
  given Show[Intels] = Show[List[Intel]].contramap(_.intels.values.toList)
}
