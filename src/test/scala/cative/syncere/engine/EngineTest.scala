package cative.syncere.engine

import java.time.Instant

import scala.annotation.targetName
import cats.data.NonEmptyList
import cats.syntax.option._

import weaver.Expectations
import weaver.SimpleIOSuite
import weaver.SourceLocation

import cative.syncere.TestValues
import cative.syncere.engine.Intels.FreshIntel
import cative.syncere.filesystem.Md5
import cative.syncere.meta._

object EngineTest extends SimpleIOSuite with TestValues {
  val delete = Delete(key1)
  val download = Download(key1)
  val upload = Upload(key1)

  extension (e: Expectations) {

    /** Replaces the reported source code line. */
    def traceTo(loc: SourceLocation): Expectations =
      Expectations(
        e.run.leftMap(_.map(e => e.copy(locations = NonEmptyList.of(loc))))
      )
  }

  extension (left: Option[FreshIntel]) {
    def :+(right: Option[FreshIntel]): Intels =
      Intels.fresh(List(left, right).flatten)
  }

  extension (maybeLocal: Option[Local]) {
    def changed: Option[Local] = maybeLocal.map(_.copy(tag = changedMd5))

    def newer: Option[Local] = maybeLocal.map { l =>
      l.copy(lastChange = l.lastChange.plusSeconds(HourSeconds))
    }

    def older: Option[Local] = maybeLocal.map { l =>
      l.copy(lastChange = l.lastChange.minusSeconds(HourSeconds))
    }
  }

  extension (maybeDeleted: Option[LocallyDeleted]) {
    def later: Option[LocallyDeleted] = maybeDeleted.map { l =>
      l.copy(seen = l.seen.plusSeconds(HourSeconds))
    }
  }

  extension (maybeRemote: Option[Remote]) {
    @targetName("maybeRemoteNewer")
    def newer: Option[Remote] = maybeRemote.map { l =>
      l.copy(lastChange = l.lastChange.plusSeconds(HourSeconds))
    }
  }

  extension (intels: Intels) {
    def :=>(action: Action)(implicit loc: SourceLocation): Expectations = {
      expect(intels.actions == List(action)).traceTo(loc)
    }
  }

  object local {
    val deleted = LocallyDeleted(key1, anInstant).some
    val missing: Option[Local] = None
    val present = Local(key1, md5, anInstant).some
  }

  object remote {
    val missing = None
    val present = Remote(key1, md5, anInstant).some
  }

  // Basic cases:

  pureTest("missing files are downloaded") {
    local.missing :+ remote.present :=> download
  }

  pureTest("new files are uploaded") {
    local.present :+ remote.missing :=> upload
  }

  pureTest("if local file is newer, it is uploaded") {
    local.present.changed.newer :+ remote.present :=> upload
  }

  pureTest("if local file is older, it is downloaded over") {
    local.present.changed.older :+ remote.present :=> download
  }

  pureTest(
    "locally deleted file is re-downloaded if the remote has updated at the same time"
  ) {
    local.deleted :+ remote.present :=> download
  }

  pureTest(
    "locally deleted file is re-downloaded if the remote has updated later"
  ) {
    local.deleted :+ remote.present.newer :=> download
  }

  pureTest("locally deleting a file removes it remotely") {
    local.deleted.later :+ remote.present :=> delete
  }

  // Dubious cases:

  pureTest(
    "if the local file is modified and we have no further clues, we bias to upload"
  ) {
    local.present.changed :+ remote.present :=> upload
  }

}
