package cative.syncere.engine

import cats.syntax.option._

import cative.syncere.meta.Db
import cative.syncere.meta.*

object Engine {

  case class Reconciliation(
      actions: List[Action],
      result: Db
  )

  def unite(locals: List[Local], remotes: List[Remote]): Intels = {
    val localMap = locals.keyMap
    val remoteMap = remotes.keyMap
    val is = (localMap.keys ++ remoteMap.keys).toSet
      .map(key => (localMap.get(key), remoteMap.get(key)))
      .map {
        case (Some(l), Some(r)) => Full(l, r).some
        case (Some(l), None)    => l.some
        case (None, Some(r))    => r.some
        case _                  => None
      }
      .flatten
      .map(i => i.key -> i)
    Intels(is.toMap)
  }

  def updateLocal(intels: Intels, local: Local): Intels =
    intels.updateWith(local.key) {
      case Some(oldIntel) =>
        oldIntel match {
          case Full(l, r) => Full(local, r)
          case r: Remote  => Full(local, r)
          case _          => local
        }
      case None =>
        local
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
          if (d.seen.isAfter(r.lastChange)) Delete(d.key)
          else Download(d.key)
        case Local(k, _, _) =>
          Upload(k)
        case LocallyDeleted(key, _) =>
          Delete(key)
        case Remote(k, _, _) =>
          Download(k)
      }
      .filter(_ != NoOp)
      .toList
}
