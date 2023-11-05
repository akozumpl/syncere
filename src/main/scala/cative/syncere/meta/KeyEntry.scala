package cative.syncere.meta

import cats.Show

import io.circe.Codec

object KeyEntry {
  type Key = String
  type Tag = String

  given Show[KeyEntry] = Show.show(_.name)
}

case class KeyEntry(
    name: KeyEntry.Key,
    tag: Option[KeyEntry.Tag]
) derives Codec.AsObject {
  def isRemote: Boolean = tag.isDefined
}
