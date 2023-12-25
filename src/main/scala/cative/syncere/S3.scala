package cative.syncere

import scala.jdk.CollectionConverters.*
import cats.data.Validated
import cats.effect.IO
import cats.effect.std.Console
import cats.syntax.show._
import cats.syntax.traverse.*

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.ListObjectsRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.S3Object

import cative.syncere.engine.Action
import cative.syncere.engine.Download
import cative.syncere.engine.Upload
import cative.syncere.filesystem.Md5
import meta.Remote

object S3 {
  import Config._

  def apply(): S3 = {
    val credentials = ProfileCredentialsProvider.create(AwsConfigProfile)
    val client = S3Client
      .builder()
      .region(AwsRegion)
      .credentialsProvider(credentials)
      .build()
    new S3(client, S3Bucket)
  }
}

class S3(client: S3Client, bucket: String) {
  private val con = Console.apply[IO]

  def fetchIntels: IO[List[Remote]] = {
    val req = ListObjectsRequest.builder().bucket(bucket).build()
    for {
      iterObjs <- IO(client.listObjects(req).contents().asScala.toList)
      is <- iterObjs.traverse(_.toRemote.toIO)
    } yield is
  }

  def play(a: Action): IO[Unit] = a match {
    case Download(key) =>
      val req = GetObjectRequest.builder().bucket(bucket).key(key).build()
      con.println(show" --- downloading $key") >> IO(
        client.getObject(req, FileSystem.keyToPath(key))
      ).as(())
    case Upload(key) =>
      val req = PutObjectRequest.builder().bucket(bucket).key(key).build()
      con.println(show" --- uploading $key") >> IO(
        client.putObject(req, FileSystem.keyToPath(key))
      ).as(())
    case _ => IO.unit
  }

  def playAll(as: List[Action]): IO[Unit] = as.traverse(play).as(())

}

extension (s3Obj: S3Object) {
  def toRemote: Validated[Throwable, Remote] =
    Md5
      .fromS3Etag(s3Obj.eTag())
      .map(md5 => Remote(s3Obj.key(), md5, s3Obj.lastModified))

}
