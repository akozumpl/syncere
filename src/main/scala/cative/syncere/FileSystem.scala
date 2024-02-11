package cative.syncere

import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant

import scala.jdk.CollectionConverters._
import cats.effect.IO
import cats.syntax.traverse._

import cative.syncere.filesystem.Creation
import cative.syncere.filesystem.Deletion
import cative.syncere.filesystem.Event
import cative.syncere.filesystem.Md5
import cative.syncere.filesystem.Modification
import cative.syncere.meta.Db
import cative.syncere.meta.KeyEntry
import cative.syncere.meta.Local
import cative.syncere.meta.LocallyDeleted

object FileSystem {

  type FSIntel = Local | LocallyDeleted

  private val walkSyncFiles: IO[List[Path]] = for {
    all <- IO(
      Files
        .walk(Config.SyncPath)
    ).map(_.iterator().asScala)
    flagged <- all.toList
      .map(path => IO((path, path.toFile().isFile())))
      .sequence
    filesOnly = flagged.collect { case (path, true) => path }
  } yield filesOnly

  private def lastModified(p: Path): IO[Instant] =
    IO(Files.getLastModifiedTime(p)).map(_.toInstant)

  private def localIntel(p: Path): IO[Local] =
    for {
      md5 <- Md5.path(p)
      lastMod <- lastModified(p)
    } yield Local(
      pathToKey(Config.SyncPath)(p),
      md5,
      lastMod
    )

  /** Produces a key given the root.
    *
    * Root itself corresponds to the empty key.
    */
  private def pathToKey(root: Path)(p: Path): KeyEntry.Key =
    root.relativize(p).toString

  private def dbFromFileIterator(entries: List[KeyEntry.Key]): IO[Db] =
    IO {
      entries.map { k =>
        (k, KeyEntry(k, None))
      }.toMap
    }.map(Db.build("localfs"))

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
        .map(pathToKey(Config.SyncPath))
        .filter(_.nonEmpty)
      db <- dbFromFileIterator(refined)
    } yield db

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
    Config.SyncPath.resolve(key)

}
