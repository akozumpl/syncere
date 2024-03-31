package cative.syncere

import java.nio.file.Path
import java.time.Duration

import scala.concurrent.duration.FiniteDuration
import scala.jdk.DurationConverters._
import cats.data.Validated
import cats.syntax.apply._

import com.monovore.decline.*
import com.monovore.decline.time.*

case class Cli(
    awsConfigProfile: Option[String],
    once: Boolean,
    pollInterval: FiniteDuration,
    s3Bucket: String,
    syncPath: Path,
    wetRun: Boolean
) {
  def isForever: Boolean = !once
}

object Cli {
  val awsConfigProfile = Opts
    .option[String](
      "aws-profile",
      help = "AWS config profile to use.",
      short = "a"
    )
    .orNone
  val once =
    Opts
      .flag(
        "once",
        "Resolve any transfers to synchronize with the remote, perform them, then exit."
      )
      .orFalse
  val pollInterval = Opts
    .option[Duration](
      "poll-interval",
      help = "How often to poll S3 for updates, e.g. PT1H.",
      short = "p"
    )
    .map(_.toScala)
    .withDefault(Config.DefaultPollInterval)
  val s3Bucket = Opts.argument[String]("s3-bucket")
  val syncPath = Opts.argument[Path]("sync-dir")
  val wetRun =
    Opts.flag("wetRun", "Actually execute any transfers.", "w").orFalse

  val cmd = Command(
    "syncere",
    "Yet another S3 syncbox imitation."
  )((awsConfigProfile, once, pollInterval, s3Bucket, syncPath, wetRun).tupled)
    .map(Cli.apply.tupled)

  def parse(args: List[String]): Validated[CliError, Cli] =
    Validated.fromEither(cmd.parse(args).left.map(CliError.apply))

}
