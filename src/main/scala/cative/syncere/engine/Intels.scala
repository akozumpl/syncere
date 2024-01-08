package cative.syncere.engine

import cats.Show
import cats.syntax.contravariant.*

import cative.syncere.engine.Intels.FreshIntel
import cative.syncere.given
import cative.syncere.meta.KeyEntry.Key
import cative.syncere.meta.*

case class Intels(intels: Map[Key, Intel]) {
  def actions = Engine.actions(this)

  def absorb(i: FreshIntel): Intels = i match {
    case l: Local           => Engine.updateLocal(this, l)
    case ld: LocallyDeleted => Engine.updateLocallyDeleted(this, ld)
    case r: Remote          => Engine.updateRemote(this, r)
  }

  def absorbAction(a: Action): Intels = Engine.actionResult(a) match {
    case Some(fresh) => absorb(fresh)
    case None        => this
  }

  def absorbAll(is: List[FreshIntel]) =
    is.foldLeft(this) { case (i, fi) => i.absorb(fi) }

  def absorbAllActions(as: List[Action]) =
    as.foldLeft(this) { case (i, a) => i.absorbAction(a) }

  def updateOrElse(key: Key)(f: Intel => Intel)(orElse: => Intel): Intels = {
    val newIntel = intels.get(key) match {
      case Some(intel) => f(intel)
      case None        => orElse
    }
    Intels(intels.updated(key, newIntel))
  }
}

object Intels {
  given Show[Intels] = Show[List[Intel]].contramap(_.intels.values.toList)

  type FreshIntel = Local | LocallyDeleted | Remote

  val empty: Intels = Intels(Map.empty)

  def fresh(freshIntels: List[FreshIntel]): Intels =
    empty.absorbAll(freshIntels)

}
