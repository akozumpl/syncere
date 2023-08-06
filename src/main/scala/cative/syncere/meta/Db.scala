package cative.syncere.meta

import cats.Show

import java.nio.file.Path

class Db(val keys: Map[KeyEntry.Key, KeyEntry])

object Db {
  given showInstance: Show[Db] = Show { db =>
    "===show Db:\n" + db.keys.map(_.toString).mkString("\n")
  }

}
