package cative.syncere.engine

import cats.Show
import cats.syntax.contravariant.*
import cats.syntax.option.*

import cative.syncere.given
import cative.syncere.meta.KeyEntry.Key
import cative.syncere.meta.*

case class Intels(intels: Map[Key, Intel]) {
  def actions = Engine.actions(this)

  def absorb(i: FreshIntel): Intels = i match {
    case l: Local            => Engine.updateLocal(this, l)
    case ld: LocallyDeleted  => Engine.updateLocallyDeleted(this, ld)
    case r: Remote           => Engine.updateRemote(this, r)
    case rs: RemoteSnapshot  => Engine.updateRemoteSnapshot(this, rs)
    case rd: RemotelyDeleted => Engine.updateRemotelyDeleted(this, rd)
  }

  def absorbAll(is: List[FreshIntel]) =
    is.foldLeft(this) { case (i, fi) => i.absorb(fi) }

  def updateOrElse(key: Key)(f: Intel => Intel)(orElse: => Intel): Intels =
    updateWith(key) {
      case Some(intel) => f(intel).some
      case None        => orElse.some
    }

  def updateWith(key: Key)(f: Option[Intel] => Option[Intel]): Intels =
    Intels(intels.updatedWith(key)(f))
}

object Intels {
  given Show[Intels] = Show[List[Intel]].contramap(_.intels.values.toList)

  val empty: Intels = Intels(Map.empty)

  def fresh(freshIntels: List[FreshIntel]): Intels =
    empty.absorbAll(freshIntels)

}
