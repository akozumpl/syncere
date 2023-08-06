package cative.syncere

import cative.syncere.meta.Db
import cative.syncere.meta.KeyEntry

import java.nio.file.Files
import java.nio.file.Path
import scala.jdk.CollectionConverters._
import scala.jdk.StreamConverters._

object FileSystem {
  /** Produces a key given the root.
    *
    * Root itself corresponds to the empty key.
    */
  private def pathToKey(root: Path)(p: Path): KeyEntry.Key =
    root.relativize(p).toString

  def dbFromFileIterator(entries: Iterator[KeyEntry.Key]): Db = {
    val entriesMap = entries.map { k =>
      (k, KeyEntry(k, None))
    }.toMap
    Db(entriesMap)
  }

  def fetchDbLocal(): Db = {
    val iterator = Files
      .walk(Config.SyncPath)
      .iterator()
      .asScala
      .map(pathToKey(Config.SyncPath))
      .filter(_.nonEmpty)
    FileSystem.dbFromFileIterator(iterator)
  }

}
