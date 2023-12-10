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

  test("Generates MD5 checksum of a filesystem file.") {
    Md5.resourcePath(ResourceName).map(md5 => expect(md5.show == ExpectedMd5))
  }

  test("Loads from an S3 tag.") {
    for {
      regularTag <- Md5.resourcePath(ResourceName)
      s3tag1 <- Md5.fromS3Etag(""""048ace47d7136fff5004dcb60dfe4e0a"""")
      s3tag2 <- Md5.fromS3Etag(""""deeece47d7136fff5004dcb60dfe4e0a"""")
    } yield expect(s3tag1 == regularTag) and expect(s3tag2 != regularTag)
  }

  test("Has strict expectations on the format of an S3 tag.") {
    Md5
      .fromS3Etag("bogus")
      .as(false)
      .recover { case Md5Error(_) => true }
      .map(x => expect(x))
  }

}
