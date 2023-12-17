package cative.syncere.filesystem

import java.io.FileInputStream
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest

import scala.util.Try
import cats.Show
import cats.effect.IO
import cats.syntax.monadError._

case class Md5(digest: List[Byte]) {
  def stringDigest: String =
    digest.map(b => String.format("%02x", b)).mkString("")
}

object Md5 {
  given Show[Md5] = Show.show(_.stringDigest)

  def fromS3Etag(etag: String): IO[Md5] = {
    val err = Md5Error(s"Unexpected etag format: $etag")
    for {
      _ <- IO.raiseUnless(etag.startsWith("\""))(err)
      _ <- IO.raiseUnless(etag.endsWith("\""))(err)
      stripped = etag.drop(1).dropRight(1)
      res <- fromString(stripped).adaptError { case _ => err }
    } yield res
  }

  def fromString(s: String): IO[Md5] =
    IO.fromTry(Try(fromStringUnsafe(s)))

  // can throw
  def fromStringUnsafe(s: String): Md5 = {
    val err = Md5Error(s"Invalid md5 string: $s")
    if (s.length != 32) {
      throw err
    }
    Md5(s.grouped(2).map(s => Integer.parseUnsignedInt(s, 16).toByte).toList)
  }

  def path(path: Path): IO[Md5] =
    for {
      md <- IO(MessageDigest.getInstance("MD5"))
      bytes <- IO(new FileInputStream(path.toFile)).bracket(is =>
        IO(is.readAllBytes())
      )(is => IO(is.close()))
      digest <- IO {
        md.update(bytes)
        md.digest()
      }
    } yield Md5(digest.toList)

  def resourcePath(resourceName: String): IO[Md5] =
    for {
      p <- IO(Paths.get(getClass.getResource(resourceName).toURI()))
      md5 <- path(p)
    } yield md5

}
