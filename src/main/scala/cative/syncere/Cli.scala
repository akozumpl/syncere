package cative.syncere

import cats.data.Validated
import cats.syntax.apply._

import com.monovore.decline.*

case class Cli(
    wetRun: Boolean,
    once: Boolean
)

object Cli {
  val wetRun =
    Opts.flag("wetRun", "Actually execute any transfers.").orFalse
  val once =
    Opts
      .flag(
        "once",
        "Resolve any transfers to synchronize with the remote, perform them, then exit."
      )
      .orFalse
  val cmd = Command(
    "syncere",
    "Yet another S3 syncbox imitation."
  )((wetRun, once).tupled).map(Cli.apply)

  def parse(args: List[String]): Validated[CliError, Cli] =
    Validated.fromEither(cmd.parse(args).left.map(CliError.apply))

}
