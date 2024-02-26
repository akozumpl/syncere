package cative.syncere.filesystem

import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant

import scala.jdk.CollectionConverters._
import cats.effect.IO
import cats.syntax.monadError._
import cats.syntax.traverse._

import cative.syncere.*
import cative.syncere.meta.Db
import cative.syncere.meta.KeyEntry
import cative.syncere.meta.Local
import cative.syncere.meta.LocallyDeleted

case class SyncDir(syncPath: Path) {
  private val walkSyncFiles: IO[List[Path]] = for {
    all <- IO(
      Files
        .walk(syncPath)
    ).map(_.iterator().asScala)
    flagged <- all.toList
      .map(path => IO.blocking((path, path.toFile().isFile())))
      .sequence
    filesOnly = flagged.collect { case (path, true) => path }
  } yield filesOnly

  private def localIntel(p: Path): IO[Local] =
    for {
      md5 <- Md5.path(p)
      lastMod <- lastModified(p)
    } yield Local(
      pathToKey(syncPath)(p),
      md5,
      lastMod
    )

  /** Deletes the key from the FS and yields the time when the deletion
    * happened.
    */
  def deleteKey(key: KeyEntry.Key): IO[Instant] = for {
    when <- IO.realTimeInstant
    _ <- IO.blocking(Files.delete(keyToPath(key)))
  } yield when

  def fetchDbLocal: IO[Db] =
    for {
      raw <- walkSyncFiles
      refined = raw
        .map(pathToKey(syncPath))
        .filter(_.nonEmpty)
    } yield dbFromFileIterator(refined)

  def fetchIntels: IO[List[Local]] =
    for {
      iter <- walkSyncFiles
      list <- iter.toList.traverse(localIntel)
    } yield list

  def intelForEvent(event: Event): IO[FSIntel] = event match {
    case e @ (_: Creation | _: Modification) =>
      localIntel(event.path)
    case Deletion(key, _) =>
      IO.realTimeInstant.map(seen => LocallyDeleted(key, seen))
  }

  def intelsSansProblems(events: List[Event]): IO[List[FSIntel]] =
    events.flatTraverse { event =>
      intelForEvent(event).map(List(_)).recoverWith {
        case e: FileNotFoundException =>
          errorln(s"Ignored local read issue: $e").as(List.empty)
      }
    }

  def keyToPath(key: KeyEntry.Key): Path =
    syncPath.resolve(key)

  def setLastModified(key: KeyEntry.Key, time: Instant): IO[Unit] = {
    val file = keyToPath(key).toFile
    IO.blocking(
      file.setLastModified(time.getEpochMillis)
    ).reject { case false =>
      new FilesystemError("Unable to set last modified timestamp.")
    }.as(())
  }

}
