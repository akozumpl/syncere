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
      .toList
    Intels(is)
  }

  def actions(i: Intels): List[Action] =
    i.intels
      .map {
        case Full(l, r) if (l.tag.stringDigest != r.tag) =>
          println(l.tag.stringDigest + "\n" + r.tag)
          Upload(l.key)
        case Local(k, _, _) =>
          Upload(k)
        case Remote(k, _) =>
          Download(k)
        case _ =>
          NoOp
      }
      .filter(_ != NoOp)
      .toList
}
