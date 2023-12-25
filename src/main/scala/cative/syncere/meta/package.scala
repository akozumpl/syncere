package cative.syncere.meta

import cative.syncere.meta.KeyEntry.Key

extension [I <: Intel](l: List[I]) {
  def keyMap: Map[Key, I] = l.map(l => l.key -> l).toMap
}
