package cative.syncere

import java.nio.file.Path

import software.amazon.awssdk.regions.{Region => R};

object Config {
  val AwsConfigProfile = "syncere"
  val AwsRegion = R.EU_WEST_2

  val Home = Path.of(System.getProperty("user.home"))
  val S3Bucket = "syncere"
  val DbPath = Home.resolve(".syncere/db.json")
}
