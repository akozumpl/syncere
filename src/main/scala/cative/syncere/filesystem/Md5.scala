package cative.syncere.filesystem

import java.io.FileInputStream
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Instant

import cats.Show
import cats.effect.IO

case class Md5(digest: List[Byte], at: Instant) {
  def stringDigest: String =
    digest.map(b => String.format("%02x", b)).mkString("")
}

object Md5 {
  given Show[Md5] = Show.show(_.stringDigest)

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
      at <- IO.realTimeInstant
    } yield Md5(digest.toList, at)

}
