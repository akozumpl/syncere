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
    case f: Full =>
      f.show
    case f: FullLocallyDeleted =>
      f.show
    case l: Local =>
      l.show
    case d: LocallyDeleted =>
      d.show
    case r: Remote =>
      r.show
  }
}

case class Full(local: Local, remote: Remote) extends Intel {
  override def key: Key = local.key
}

object Full {
  given Show[Full] =
    Show.show(f => show"Full intel: ${f.local} / ${f.remote}")
}

case class FullLocallyDeleted(locallyDeleted: LocallyDeleted, remote: Remote)
    extends Intel {
  override def key: Key = locallyDeleted.key
}

object FullLocallyDeleted {
  given Show[FullLocallyDeleted] =
    Show.show(f =>
      show"Local deletion with existing remote intel: ${f.locallyDeleted} / ${f.remote}"
    )
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

case class LocallyDeleted(key: Key, seen: Instant) extends Intel

object LocallyDeleted {
  given Show[LocallyDeleted] =
    Show.show(l => show"Locally deleted intel: ${l.key} witnessed ${l.seen}.")
}

case class Remote(key: Key, tag: Md5, lastChange: Instant) extends Intel

object Remote {
  given Show[Remote] =
    Show.show(r => show"Remote intel: ${r.key} ${r.tag} ${r.lastChange}")
}
