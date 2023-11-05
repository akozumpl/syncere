package cative.syncere

import java.nio.file.Files
import java.nio.file.Path

import scala.jdk.CollectionConverters._
import scala.jdk.StreamConverters._
import cats.effect.IO

import cative.syncere.meta.Db
import cative.syncere.meta.KeyEntry

object FileSystem {

  /** Produces a key given the root.
    *
    * Root itself corresponds to the empty key.
    */
  private def pathToKey(root: Path)(p: Path): KeyEntry.Key =
    root.relativize(p).toString

  def dbFromFileIterator(entries: Iterator[KeyEntry.Key]): IO[Db] =
    IO {
      entries.map { k =>
        (k, KeyEntry(k, None))
      }.toMap
    }.map(Db.build("localfs"))

  def fetchDbLocal(): IO[Db] =
    for {
      raw <- IO(
        Files
          .walk(Config.SyncPath)
          .iterator()
          .asScala
      )
      refined = raw
        .map(pathToKey(Config.SyncPath))
        .filter(_.nonEmpty)
      db <- FileSystem.dbFromFileIterator(refined)
    } yield db

  def keyToPath(key: KeyEntry): Path =
    Config.SyncPath.resolve(key.name)

}
