package cative.syncere.engine

import cats.syntax.option.*

import cative.syncere.meta.Db
import cative.syncere.meta.*

object Engine {

  case class Reconciliation(
      actions: List[Action],
      result: Db
  )

  private[engine] def updateLocal(intels: Intels, local: Local): Intels =
    intels.updateOrElse(local.key) {
      case Full(l, r)                => Full(local, r)
      case FullLocallyDeleted(ld, r) => Full(local, r)
      case r: Remote                 => Full(local, r)
      case _                         => local
    }(local)

  private[engine] def updateLocallyDeleted(
      intels: Intels,
      deleted: LocallyDeleted
  ): Intels =
    intels.updateOrElse(deleted.key) {
      case Full(l, r) => FullLocallyDeleted(deleted, r)
      case r: Remote  => FullLocallyDeleted(deleted, r)
      case _          => deleted
    }(deleted)

  private[engine] def updateRemote(intels: Intels, remote: Remote): Intels =
    intels.updateOrElse(remote.key) {
      case Full(l, r)         => Full(l, remote)
      case l: Local           => Full(l, remote)
      case ld: LocallyDeleted => FullLocallyDeleted(ld, remote)
      case _                  => remote
    }(remote)

  private[engine] def updateRemotelyDeleted(
      intels: Intels,
      deleted: RemotelyDeleted
  ): Intels =
    intels.updateWith(deleted.key) {
      case Some(oldIntel) =>
        oldIntel match {
          case FullLocallyDeleted(locallyDeleted, r) => None
          case Full(l, r)           => FullRemotelyDeleted(l, deleted).some
          case l: Local             => FullRemotelyDeleted(l, deleted).some
          case LocallyDeleted(_, _) => None
          case _                    => None
        }
      case None =>
        None
    }

  def action(i: Intel): Action = i match {
    case Full(l, r) =>
      if (l.tag != r.tag) {
        if (r.lastChange.isAfter(l.lastChange)) Download(r)
        else Upload(l)
      } else NoOp
    case FullLocallyDeleted(d, r) =>
      if (d.seen.isAfter(r.lastChange)) DeleteRemotely(d.key)
      else Download(r)
    case FullRemotelyDeleted(l, d) =>
      DeleteLocally(l.key)
    case l: Local =>
      Upload(l)
    case LocallyDeleted(key, _) =>
      DeleteRemotely(key)
    case r: Remote =>
      Download(r)
    case RemotelyDeleted(key) =>
      DeleteLocally(key)
  }

  def actions(is: Intels): List[Action] =
    is.intels.values
      .map(action)
      .filter(_ != NoOp)
      .toList
}
