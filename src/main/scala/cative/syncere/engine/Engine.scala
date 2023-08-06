package cative.syncere.engine

import cative.syncere.meta.Db

object Engine {

  case class Reconciliation(
      actions: List[Action],
      result: Db
  )

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
