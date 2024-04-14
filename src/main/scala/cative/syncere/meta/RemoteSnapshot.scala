package cative.syncere.meta

import cative.syncere.meta.KeyEntry.Key

/** Mere sequence of remote updates cannot capture a deletion. */
case class RemoteSnapshot(remotes: List[Remote]) {
  def keySet: Set[Key] = remotes.map(_.key).toSet
}

object RemoteSnapshot {
  def empty: RemoteSnapshot = RemoteSnapshot(List.empty)
}
