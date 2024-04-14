package cative.syncere.engine

import java.time.Instant

import scala.annotation.targetName
import cats.data.NonEmptyList
import cats.syntax.option.*

import weaver.Expectations
import weaver.SimpleIOSuite
import weaver.SourceLocation

import cative.syncere.TestValues
import cative.syncere.filesystem.Md5
import cative.syncere.meta.FreshIntel
import cative.syncere.meta.*

object EngineFTest extends SimpleIOSuite with TestValues {
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
            expect(l.key == l2.key)
          }
        case a =>
          expect(actions == a.toList)
      }
    }

    private def intelActionResult(i: Intel): Option[FreshIntel] =
      Engine.action(i) match {
        case d @ DeleteLocally(_)    => d.result(anInstant).some
        case d @ DeleteRemotely(key) => d.result.some
        case d @ Download(remote)    => d.result.some
        case NoOp                    => None
        case u @ Upload(local)       => u.result.some
      }

    /** Simulates actions taking place. */
    private def played: Intels = {
      val results = intels.intels.values.flatMap(intelActionResult).toList
      intels.absorbAll(results)
    }

    /** Verifies the type and the key of the action matches the expectation */
    def :=>(rhAction: Option[Action])(implicit
        loc: SourceLocation
    ): Expectations =
      mainExpectations(rhAction)
        .and(expect(played.actions.isEmpty))
        .traceTo(loc)
  }

  object local {
    val deleted = LocallyDeleted(key1, anInstant).some
    val missing: Option[Local] = None
    val present = Local(key1, md5, anInstant).some
  }

  object remote {
    val missing = None
    val deleted = RemotelyDeleted(key1).some
    val present = Remote(key1, md5, anInstant).some
  }

  val deleteLocal = DeleteLocally(key1).some
  val deleteRemote = DeleteRemotely(key1).some
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
    local.deleted.later :+ remote.present :=> deleteRemote
  }

  pureTest("Re-creating a file after its deletion results in no op.") {
    remote.present :+ local.deleted :+ local.present :=> noOp
  }

  pureTest("Remotely deleting a file removes it locally.") {
    local.present :+ remote.deleted :=> deleteLocal
  }

  // Dubious cases:

  pureTest(
    "If the local file is modified and we have no further clues, we bias to upload."
  ) {
    local.present.changed :+ remote.present :=> upload
  }

  pureTest(
    "If we learn about local update in the same moment as remote deletion, we bias to re-upload."
  ) {
    local.present :+ remote.deleted :+ local.present.changed :=> upload
  }

}
