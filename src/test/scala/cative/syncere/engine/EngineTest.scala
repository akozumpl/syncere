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
  val download = Download(key)
  val upload = Upload(key)

  extension (e: Expectations) {

    /** Replaces the reported source code line. */
    def traceTo(loc: SourceLocation): Expectations =
      Expectations(
        e.run.leftMap(_.map(e => e.copy(locations = NonEmptyList.of(loc))))
      )
  }

  extension (maybeLocal: Option[Local]) {

    def :+(remote: Option[Remote]): Combined = Combined(maybeLocal, remote)

    def changed: Option[Local] = maybeLocal.map(_.copy(tag = changedMd5))

    def newer: Option[Local] = maybeLocal.map { l =>
      l.copy(lastChange = l.lastChange.plusSeconds(HourSeconds))
    }

    def older: Option[Local] = maybeLocal.map { l =>
      l.copy(lastChange = l.lastChange.minusSeconds(HourSeconds))
    }
  }

  case class Combined(local: Option[Local], remote: Option[Remote]) {
    def :=>(action: Action)(implicit loc: SourceLocation): Expectations = {
      val united = Intels.fresh(List(local, remote).flatten)
      expect(Engine.actions(united) == List(action)).traceTo(loc)
    }
  }

  object local {
    val missing: Option[Local] = None
    val present = Local(key, md5, anInstant).some
  }

  object remote {
    val missing = None
    val present = Remote(key, md5, anInstant).some
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
