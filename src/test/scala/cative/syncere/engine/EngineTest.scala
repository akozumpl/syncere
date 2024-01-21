package cative.syncere.engine

import java.time.Instant

import scala.annotation.targetName
import cats.data.NonEmptyList
import cats.syntax.option.*

import weaver.Expectations
import weaver.SimpleIOSuite
import weaver.SourceLocation

import cative.syncere.TestValues
import cative.syncere.engine.Intels.FreshIntel
import cative.syncere.filesystem.Md5
import cative.syncere.meta.*

object EngineTest extends SimpleIOSuite with TestValues {
  extension (e: Expectations) {

    /** Appends the reported source code line. */
    def traceTo(loc: SourceLocation): Expectations =
      Expectations(
        e.run.leftMap(_.map(e => e.copy(locations = e.locations.append((loc)))))
      )
  }

  extension (left: Option[FreshIntel]) {
    def :+(right: Option[FreshIntel]): Intels =
      Intels.empty :+ left :+ right
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
    def :+(intel: Option[FreshIntel]): Intels = intels.absorbAll(intel.toList)

    private def mainExpectations(rhAction: Option[Action]): Expectations = {
      val actions = intels.actions
      rhAction match {
        case None | Some(NoOp) =>
          expect(actions.isEmpty)
        case Some(Download(r)) =>
          matches(actions) { case Download(r2) :: Nil =>
            expect(r2.key == r.key)
          }
        case Some(Upload(l)) =>
          matches(actions) { case Upload(l2) :: Nil =>
            expect(l2.key == l2.key)
          }
        case a =>
          expect(actions == a.toList)
      }
    }

    /** Verifies the type and the key of the action matches the expectation */
    def :=>(rhAction: Option[Action])(implicit
        loc: SourceLocation
    ): Expectations =
      mainExpectations(rhAction)
        .and(expect(intels.absorbAllActions(intels.actions).actions.isEmpty))
        .traceTo(loc)
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

  val delete = DeleteRemotely(key1).some
  val download = remote.present.map(Download.apply)
  val noOp = None
  val upload = local.present.map(Upload.apply)

  // Basic cases:

  pureTest("Missing files are downloaded.") {
    local.missing :+ remote.present :=> download
  }

  pureTest("New files are uploaded.") {
    local.present :+ remote.missing :=> upload
  }

  pureTest("If local file is newer, it is uploaded.") {
    local.present.changed.newer :+ remote.present :=> upload
  }

  pureTest("If local file is older, it is downloaded.") {
    local.present.changed.older :+ remote.present :=> download
  }

  pureTest(
    "Locally deleted file is re-downloaded if the remote has updated at the same time."
  ) {
    local.deleted :+ remote.present :=> download
  }

  pureTest(
    "Locally deleted file is re-downloaded if the remote has updated later."
  ) {
    local.deleted :+ remote.present.newer :=> download
  }

  pureTest("Locally deleting a file removes it remotely.") {
    local.deleted.later :+ remote.present :=> delete
  }

  pureTest("Re-creating a file after its deletion results in no op.") {
    remote.present :+ local.deleted :+ local.present :=> noOp
  }

  // Dubious cases:

  pureTest(
    "If the local file is modified and we have no further clues, we bias to upload."
  ) {
    local.present.changed :+ remote.present :=> upload
  }

}
