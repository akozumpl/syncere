package cative.syncere.meta

import java.nio.file.Files

import cats.effect.IO

import cative.syncere.Config

object Serializer {
  type Payload = Array[Byte]

  val loaded: IO[Db] =
    for {
      payload <- IO(Files.readAllBytes(Config.DbPath))
      db <- Db.deserialize(payload)
    } yield db

  def store(db: Db): IO[Unit] = {
    val payload = db.serialize
    IO(Files.write(Config.DbPath, payload))
  }

}
