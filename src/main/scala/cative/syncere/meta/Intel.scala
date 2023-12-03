package cative.syncere.meta

import java.time.Instant

import cats.Show
import cats.syntax.show._

import cative.syncere.filesystem.Md5
import cative.syncere.given
import cative.syncere.meta.KeyEntry.*

sealed trait Intel {
  def key: Key
}

object Intel {
  given Show[Intel] = Show.show {
    case l: Local =>
      l.show
    case i =>
      s"Intel(${i.key})"
  }
}

case class Full(local: Local, remote: Remote) extends Intel {
  override def key: Key = local.key
}

case class Local(
    key: Key,
    tag: Md5,
    lastChange: Instant
) extends Intel

object Local {
  given Show[Local] =
    Show.show(l =>
      show"Local intel: ${l.key} ${l.tag}, changed ${l.lastChange}"
    )
}

/** Remembers state from the last run of the service.
  *
  * Useful to tell what files have changed while the service was shut down.
  */
case class Recall(key: Key, tag: Tag, time: Instant) extends Intel

case class Remote(key: Key, tag: Tag) extends Intel
