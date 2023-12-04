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

  def reconcile(local: Db, remote: Db): List[Action] = {
    val toDownload = remote.keys.values.filter { entry =>
      !local.keys.isDefinedAt(entry.name)
    }

    val remoteTags = remote.projectTags
    val toUpload = local.keys.values.filter { entry =>
      entry.tag match {
        case Some(tag) =>
          remoteTags.get(entry.name) match {
            case Some(correspondingTag) =>
              tag != correspondingTag
            case None =>
              true
          }

        case None =>
          true
      }
    }

    val concat = toDownload.map(Download.apply) ++
      toUpload.map(Upload.apply)
    concat.toList
  }
}
