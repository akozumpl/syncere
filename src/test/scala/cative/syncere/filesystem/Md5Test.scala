package cative.syncere.filesystem

import java.nio.file.Path
import java.nio.file.Paths

import cats.data.Validated.Invalid
import cats.effect.IO
import cats.syntax.show._

import weaver.SimpleIOSuite

import cative.syncere.toIO

object Md5Test extends SimpleIOSuite {
  private val ResourceName = "/milan.txt"
  private val ExpectedMd5 = "f63f1693a02dd66b3f24db4beb28fb9f"

  def resourcePath(resourceName: String): IO[Path] =
    IO(Paths.get(getClass.getResource(resourceName).toURI()))

  test("Generates MD5 checksum of a filesystem file.") {
    Md5.resourcePath(ResourceName).map(md5 => expect(md5.show == ExpectedMd5))
  }

  test("Loads from an S3 tag.") {
    for {
      regularTag <- Md5.resourcePath(ResourceName)
      s3tag1 <- Md5.fromS3Etag(""""f63f1693a02dd66b3f24db4beb28fb9f"""").toIO
      s3tag2 <- Md5.fromS3Etag(""""deeece47d7136fff5004dcb60dfe4e0a"""").toIO
    } yield expect(s3tag1 == regularTag) and expect(s3tag2 != regularTag)
  }

  pureTest("Has strict expectations on the format of an S3 tag.") {
    matches(Md5.fromS3Etag("bogus")) { case Invalid(Md5Error(_)) =>
      expect(true)
    }
  }

}
