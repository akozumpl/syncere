package cative.syncere.meta

import java.nio.file.Path

import cats.Show
import cats.effect.IO
import cats.syntax.show._

import io.circe.Codec
import io.circe.Json
import io.circe.parser
import io.circe.syntax._

object Db {
  given Show[Db] = Show { db =>
    s"=== show Db ${db.name}:\n" + db.keys.map(_.toString).mkString("\n")
  }

  def build(name: String)(keys: Map[KeyEntry.Key, KeyEntry]): Db =
    Db(name, keys)

  def deserialize(payload: Serializer.Payload): IO[Db] = {
    val res = for {
      json <- parser.parse(new String(payload))
      db <- json.as[Db]
    } yield db
    IO.fromEither(res)
  }
}

case class Db(name: String, keys: Map[KeyEntry.Key, KeyEntry])
    derives Codec.AsObject {
  def json: Json = this.asJson

  def projectTags: Map[KeyEntry.Key, KeyEntry.Tag] =
    keys.collect { case (k, KeyEntry(_, Some(e))) => (k, e) }

  def serialize: Serializer.Payload = json.show.getBytes()
}
