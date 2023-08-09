package cative.syncere.meta

import java.nio.file.Path

import cats.Show
import cats.syntax.show._

import io.circe.Codec
import io.circe.Json
import io.circe.syntax._

object Db {
  given Show[Db] = Show { db =>
    "=== show Db:\n" + db.keys.map(_.toString).mkString("\n")
  }
}

case class Db(val keys: Map[KeyEntry.Key, KeyEntry]) derives Codec.AsObject {
  def json: Json = this.asJson

  def projectTags: Map[KeyEntry.Key, KeyEntry.Tag] =
    keys.collect { case (k, KeyEntry(_, Some(e))) => (k, e) }

  def serialize: Serializer.Payload = json.show.getBytes()
}
