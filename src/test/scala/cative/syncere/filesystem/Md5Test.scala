package cative.syncere.filesystem

import java.nio.file.Path
import java.nio.file.Paths

import cats.effect.IO
import cats.syntax.show._

import weaver.SimpleIOSuite

object Md5Test extends SimpleIOSuite {
  private val ResourceName = "/milan.txt"
  private val ExpectedMd5 = "048ace47d7136fff5004dcb60dfe4e0a"

  def resourcePath(resourceName: String): IO[Path] =
    IO(Paths.get(getClass.getResource(resourceName).toURI()))

  test("generating MD5 checksum of a filesystem file") {
    for {
      p <- resourcePath(ResourceName)
      md5 <- Md5.path(p)
    } yield expect(md5.show == ExpectedMd5)

  }
}
