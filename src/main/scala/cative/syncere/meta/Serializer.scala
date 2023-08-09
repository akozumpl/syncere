package cative.syncere.meta

import java.nio.file.Files

import cats.effect.IO

import cative.syncere.Config

object Serializer {
  type Payload = Array[Byte]

  def store(db: Db): IO[Unit] = {
    val payload = db.serialize
    val path = Config.DbPath.resolve("db.json")
    IO(Files.write(path, payload))
  }
}
