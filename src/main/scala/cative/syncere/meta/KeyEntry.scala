package cative.syncere.meta

object KeyEntry {
  type Key = String
  type Tag = String
}

case class KeyEntry(
    name: KeyEntry.Key,
    tag: Option[KeyEntry.Tag]
) {
  def isRemote: Boolean = tag.isDefined
}
