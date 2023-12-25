package cative.syncere

import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant

import scala.jdk.CollectionConverters._
import cats.effect.IO
import cats.syntax.traverse._

import cative.syncere.filesystem.Md5
import cative.syncere.meta.Db
import cative.syncere.meta.KeyEntry
import cative.syncere.meta.Local

object FileSystem {
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

  def keyToPath(key: KeyEntry.Key): Path =
    Config.SyncPath.resolve(key)

}
