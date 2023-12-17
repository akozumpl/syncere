package cative.syncere.engine

import java.time.Instant

import cats.data.NonEmptyList
import cats.syntax.option._

import weaver.Expectations
import weaver.SimpleIOSuite
import weaver.SourceLocation

import cative.syncere.filesystem.Md5
import cative.syncere.meta._

object EngineTest extends SimpleIOSuite {

  val key = "fileio.mkv"
  val md5 = Md5.fromStringUnsafe("75d7a0c43d6e92130301bb29185e24f2")
  val time = Instant.parse("2030-12-03T10:15:30.00Z")

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
  }

  case class Combined(local: Option[Local], remote: Option[Remote]) {
    def :=>(action: Action)(implicit loc: SourceLocation): Expectations = {
      val united = Engine.unite(local.toList, remote.toList)
      expect(Engine.actions(united) == List(action)).traceTo(loc)
    }
  }

  object local {
    val missing: Option[Local] = None
    val present = Local(key, md5, time).some
  }

  object remote {
    val missing = None
    val present = Remote(key, md5).some
  }

  // Basic cases:

  pureTest("missing files are downloaded") {
    local.missing :+ remote.present :=> download
  }

  pureTest("new files are uploaded") {
    local.present :+ remote.missing :=> upload
  }

}
