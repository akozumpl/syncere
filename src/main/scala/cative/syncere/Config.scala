package cative.syncere

import java.nio.file.Path

import scala.concurrent.duration.*

import software.amazon.awssdk.regions.{Region => R}

object Config {
  val AwsRegion = R.EU_WEST_2

  val Home = Path.of(System.getProperty("user.home"))
  val DbPath = Home.resolve(".syncere/db.json")

  val DefaultPollInterval = 4.hours

}
