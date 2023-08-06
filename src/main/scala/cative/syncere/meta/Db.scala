package cative.syncere.meta

import java.nio.file.Path

import cats.Show

class Db(val keys: Map[KeyEntry.Key, KeyEntry]) {
  def projectTags: Map[KeyEntry.Key, KeyEntry.Tag] =
    keys.collect { case (k, KeyEntry(_, Some(e))) => (k, e) }
}

object Db {
  given showInstance: Show[Db] = Show { db =>
    "=== show Db:\n" + db.keys.map(_.toString).mkString("\n")
  }

}
