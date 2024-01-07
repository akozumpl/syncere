package cative.syncere.engine

import cats.syntax.option._

import cative.syncere.meta.Db
import cative.syncere.meta.*

object Engine {

  case class Reconciliation(
      actions: List[Action],
      result: Db
  )

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
            if (r.lastChange.isAfter(l.lastChange)) Download(l.key)
            else Upload(l.key)
          } else NoOp
        case FullLocallyDeleted(d, r) =>
          if (d.seen.isAfter(r.lastChange)) DeleteRemotely(d.key)
          else Download(d.key)
        case Local(k, _, _) =>
          Upload(k)
        case LocallyDeleted(key, _) =>
          DeleteRemotely(key)
        case Remote(k, _, _) =>
          Download(k)
      }
      .filter(_ != NoOp)
      .toList
}
