package cative.syncere

import scala.jdk.CollectionConverters.*
import cats.syntax.option._

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.core.ResponseBytes
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import software.amazon.awssdk.services.s3.model.GetObjectTaggingRequest
import software.amazon.awssdk.services.s3.model.GetObjectTaggingResponse
import software.amazon.awssdk.services.s3.model.ListObjectsRequest
import software.amazon.awssdk.services.s3.model.ListObjectsResponse
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.S3Exception
import software.amazon.awssdk.services.s3.model.S3Object
import software.amazon.awssdk.services.s3.model.Tag

import meta.KeyEntry;

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
  def list(): List[KeyEntry] = {
    val req = ListObjectsRequest.builder().bucket(bucket).build()
    client
      .listObjects(req)
      .contents()
      .asScala
      .map { s3Obj =>
        KeyEntry(s3Obj.key(), s3Obj.eTag().some)
      }
      .toList
  }

}
