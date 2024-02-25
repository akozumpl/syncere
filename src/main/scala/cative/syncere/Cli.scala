package cative.syncere

import java.nio.file.Path

import cats.data.Validated
import cats.syntax.apply._

import com.monovore.decline.*

case class Cli(
    awsConfigProfile: String,
    once: Boolean,
    s3Bucket: String,
    syncPath: Path,
    wetRun: Boolean
) {
  def isForever: Boolean = !once
}

object Cli {
  val awsConfigProfile = Opts.option[String](
    "aws-profile",
    help = "AWS config profile to use",
    short = "a"
  )
  val once =
    Opts
      .flag(
        "once",
        "Resolve any transfers to synchronize with the remote, perform them, then exit."
      )
      .orFalse
  val s3Bucket = Opts.argument[String]("s3-bucket")
  val syncPath = Opts.argument[Path]("sync-dir")
  val wetRun =
    Opts.flag("wetRun", "Actually execute any transfers.", "w").orFalse

  val cmd = Command(
    "syncere",
    "Yet another S3 syncbox imitation."
  )((awsConfigProfile, once, wetRun, syncPath, s3Bucket).tupled).map {
    case (awsConfigProfile, once, wetRun, syncPath, s3Bucket) =>
      Cli(awsConfigProfile, once, s3Bucket, syncPath, wetRun)
  }

  def parse(args: List[String]): Validated[CliError, Cli] =
    Validated.fromEither(cmd.parse(args).left.map(CliError.apply))

}
