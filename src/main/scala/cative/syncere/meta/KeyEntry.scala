package cative.syncere.meta

import io.circe.Codec

object KeyEntry {
  type Key = String
  type Tag = String
}

case class KeyEntry(
    name: KeyEntry.Key,
    tag: Option[KeyEntry.Tag]
) derives Codec.AsObject {
  def isRemote: Boolean = tag.isDefined
}
