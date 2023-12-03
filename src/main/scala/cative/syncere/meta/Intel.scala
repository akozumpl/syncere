package cative.syncere.meta

import java.time.Instant

import cative.syncere.filesystem.Md5
import cative.syncere.meta.KeyEntry.*

sealed trait Intel {
  def key: Key
}

case class Full(local: Local, remote: Remote) extends Intel {
  override def key: Key = local.key
}

case class Local(
    key: Key,
    tag: Md5,
    lastChange: Instant
) extends Intel

/** Remembers state from the last run of the service.
  *
  * Useful to tell what files have changed while the service was shut down.
  */
case class Recall(key: Key, tag: Tag, time: Instant) extends Intel

case class Remote(key: Key, tag: Tag) extends Intel
