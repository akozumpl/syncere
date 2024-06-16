package cative.syncere.filesystem

import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant

import scala.jdk.CollectionConverters._
import cats.effect.IO
import cats.effect.Resource

import cative.syncere.meta.Db
import cative.syncere.meta.KeyEntry
import cative.syncere.meta.Local
import cative.syncere.meta.LocallyDeleted

type FSIntel = Local | LocallyDeleted

def delete(p: Path): IO[Unit] =
  IO.blocking(Files.deleteIfExists(p)).as(())

private def lastModified(p: Path): IO[Instant] =
  IO.blocking(Files.getLastModifiedTime(p)).map(_.toInstant)

private def listRecursively(p: Path): IO[List[Path]] =
  Resource
    .fromAutoCloseable(
      IO.blocking(
        Files
          .walk(p)
      )
    )
    .use(stream => IO.blocking(stream.iterator().asScala.toList))

/** Produces a key given the root.
  *
  * Root itself corresponds to the empty key.
  */
private def pathToKey(root: Path)(p: Path): KeyEntry.Key =
  root.relativize(p).toString

private def dbFromFileIterator(entries: List[KeyEntry.Key]): Db = {
  val keys = entries.map { k =>
    (k, KeyEntry(k, None))
  }.toMap
  Db.build("localfs")(keys)
}
