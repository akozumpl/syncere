package cative.syncere.engine

import cats.Show
import cats.syntax.contravariant._

import cative.syncere.engine.Intels.FreshIntel
import cative.syncere.given
import cative.syncere.meta.KeyEntry.Key
import cative.syncere.meta.*

case class Intels(intels: Map[Key, Intel]) {
  def absorb(i: FreshIntel): Intels = i match {
    case l: Local           => Engine.updateLocal(this, l)
    case ld: LocallyDeleted => Engine.updateLocallyDeleted(this, ld)
    case r: Remote          => Engine.updateRemote(this, r)
  }

  def updateWith(key: Key)(f: Option[Intel] => Intel): Intels = {
    val updated = f(intels.get(key))
    Intels(intels.updated(key, updated))
  }
}

object Intels {
  given Show[Intels] = Show[List[Intel]].contramap(_.intels.values.toList)

  type FreshIntel = Local | LocallyDeleted | Remote

  val empty: Intels = Intels(Map.empty)

  def fresh(freshIntels: List[FreshIntel]): Intels =
    freshIntels.foldLeft(empty) { case (i, fi) => i.absorb(fi) }

}
