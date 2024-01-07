package cative.syncere.engine

import cats.syntax.option.*

import cative.syncere.engine.Intels.FreshIntel
import cative.syncere.meta.Db
import cative.syncere.meta.*

object Engine {

  case class Reconciliation(
      actions: List[Action],
      result: Db
  )

  /** Produces an intel we assume to be true whenever the action was successful.
    *
    * The resulting intels tend to have a stale timestamp: this is OK, an actual
    * event (on a remote refresh or a filesystem event) is fine to override this
    * one.
    */
  private[engine] def actionResult(a: Action): Option[FreshIntel] = a match {
    case DeleteLocally(_)  => None
    case DeleteRemotely(_) => None
    case Download(remote) =>
      Local(remote.key, remote.tag, remote.lastChange).some
    case NoOp          => None
    case Upload(local) => Remote(local.key, local.tag, local.lastChange).some
  }

  private[engine] def updateLocal(intels: Intels, local: Local): Intels =
    intels.updateWith(local.key) {
      case Some(oldIntel) =>
        oldIntel match {
          case Full(l, r)                => Full(local, r)
          case FullLocallyDeleted(ld, r) => Full(local, r)
          case r: Remote                 => Full(local, r)
          case _                         => local
        }
      case None =>
        local
    }

  private[engine] def updateLocallyDeleted(
      intels: Intels,
      deleted: LocallyDeleted
  ): Intels =
    intels.updateWith(deleted.key) {
      case Some(oldIntel) =>
        oldIntel match {
          case Full(l, r) => FullLocallyDeleted(deleted, r)
          case r: Remote  => FullLocallyDeleted(deleted, r)
          case _          => deleted
        }
      case None =>
        deleted
    }

  private[engine] def updateRemote(intels: Intels, remote: Remote): Intels =
    intels.updateWith(remote.key) {
      case Some(oldIntel) =>
        oldIntel match {
          case Full(l, r)         => Full(l, remote)
          case l: Local           => Full(l, remote)
          case ld: LocallyDeleted => FullLocallyDeleted(ld, remote)
          case _                  => remote
        }
      case None =>
        remote
    }

  def actions(i: Intels): List[Action] =
    i.intels.values
      .map {
        case Full(l, r) =>
          if (l.tag != r.tag) {
            if (r.lastChange.isAfter(l.lastChange)) Download(r)
            else Upload(l)
          } else NoOp
        case FullLocallyDeleted(d, r) =>
          if (d.seen.isAfter(r.lastChange)) DeleteRemotely(d.key)
          else Download(r)
        case l: Local =>
          Upload(l)
        case LocallyDeleted(key, _) =>
          DeleteRemotely(key)
        case r: Remote =>
          Download(r)
      }
      .filter(_ != NoOp)
      .toList
}
