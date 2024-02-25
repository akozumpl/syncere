package cative.syncere

import java.nio.file.Path

import cats.data.Validated
import cats.syntax.apply._

import com.monovore.decline.*

case class Cli(
    once: Boolean,
    syncPath: Path,
    wetRun: Boolean
) {
  def isForever: Boolean = !once
}

object Cli {
  val once =
    Opts
      .flag(
        "once",
        "Resolve any transfers to synchronize with the remote, perform them, then exit."
      )
      .orFalse
  val syncPath = Opts.argument[Path]("sync-dir")
  val wetRun =
    Opts.flag("wetRun", "Actually execute any transfers.", "w").orFalse

  val cmd = Command(
    "syncere",
    "Yet another S3 syncbox imitation."
  )((once, syncPath, wetRun).tupled).map(Cli.apply)

  def parse(args: List[String]): Validated[CliError, Cli] =
    Validated.fromEither(cmd.parse(args).left.map(CliError.apply))

}
