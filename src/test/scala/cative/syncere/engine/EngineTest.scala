package cative.syncere.engine

import java.time.Instant

import cats.data.NonEmptyList
import cats.syntax.option._

import weaver.Expectations
import weaver.SimpleIOSuite
import weaver.SourceLocation

import cative.syncere.TestValues
import cative.syncere.filesystem.Md5
import cative.syncere.meta._

object EngineTest extends SimpleIOSuite with TestValues {
  val download = Download(key1)
  val upload = Upload(key1)

  extension (e: Expectations) {

    /** Replaces the reported source code line. */
    def traceTo(loc: SourceLocation): Expectations =
      Expectations(
        e.run.leftMap(_.map(e => e.copy(locations = NonEmptyList.of(loc))))
      )
  }

  extension (maybeLocal: Option[Local]) {
    def :+(maybeRemote: Option[Remote]): Intels =
      Intels.fresh(List(maybeLocal, maybeRemote).flatten)

    def changed: Option[Local] = maybeLocal.map(_.copy(tag = changedMd5))

    def newer: Option[Local] = maybeLocal.map { l =>
      l.copy(lastChange = l.lastChange.plusSeconds(HourSeconds))
    }

    def older: Option[Local] = maybeLocal.map { l =>
      l.copy(lastChange = l.lastChange.minusSeconds(HourSeconds))
    }
  }

  extension (intels: Intels) {
    def :=>(action: Action)(implicit loc: SourceLocation): Expectations = {
      expect(intels.actions == List(action)).traceTo(loc)
    }
  }

  object local {
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

  // Dubious cases:

  pureTest(
    "if the local file is modified and we have no further clues, we bias to upload"
  ) {
    local.present.changed :+ remote.present :=> upload
  }

}
