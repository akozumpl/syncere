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
    intels.updateWith(deleted.key) {
      case Some(Full(l, r)) => FullLocallyDeleted(deleted, r).some
      case Some(r: Remote)  => FullLocallyDeleted(deleted, r).some
      case Some(FullRemotelyDeleted(_, _)) => None
      case Some(RemotelyDeleted(_))        => None
      case _                               => deleted.some
    }

  private[engine] def updateRemote(intels: Intels, remote: Remote): Intels =
    intels.updateOrElse(remote.key) { intel =>
      intel match {
        case Full(l, r)         => Full(l, remote)
        case l: Local           => Full(l, remote)
        case ld: LocallyDeleted => FullLocallyDeleted(ld, remote)
        case _                  => remote
      }
    }(remote)

  private[engine] def updateRemoteSnapshot(
      intels: Intels,
      remoteSnapshot: RemoteSnapshot
  ): Intels = {
    val absorbed = intels.absorbAll(remoteSnapshot.remotes)
    // deal with the missing keys:
    (intels.intels.keySet -- remoteSnapshot.keySet)
      .foldLeft(absorbed) { case (intels, key) =>
        intels.updateWith(key) {
          case Some(oldIntel) =>
            oldIntel match {
              case FullLocallyDeleted(_, _) => None
              case Full(l, _) =>
                FullRemotelyDeleted(l, RemotelyDeleted(key)).some
              case LocallyDeleted(_, _) => None
              case Remote(_, _, _)      => None
              case x                    => x.some
            }
          case None => None
        }
      }
  }

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
      // Two reasons to avoid deleting remotely here:
      // 1. Under a race condition we might delete a genuine new remote file
      // 2. If the local deletion was an action of remote deletion, the request is unnecessary
      NoOp
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
