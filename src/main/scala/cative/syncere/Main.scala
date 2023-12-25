package cative.syncere

import java.nio.file.FileSystems
import java.nio.file.WatchService
import java.nio.file.{StandardWatchEventKinds => K}

import scala.jdk.CollectionConverters.*
import cats.Show
import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.syntax.flatMap.*

import cative.syncere.engine.Engine
import cative.syncere.filesystem.Event
import cative.syncere.given
import cative.syncere.meta._

object Main extends IOApp {
  val con = IO.consoleForIO

  def printShow[A: Show](a: A): IO[Unit] =
    con.println(a)

  def printTagged[A: Show](tag: String, a: A): IO[Unit] =
    con.println(s"--- $tag: ---") >> printShow(a)

  override def run(args: List[String]): IO[ExitCode] = {
    val s3 = S3()

    val res = for {
      cli <- Cli.parse(args).toIO

      remoteDb <- s3.fetchIntels
      localDb <- FileSystem.fetchIntels
      unified = Engine.unite(localDb, remoteDb)
      actions = Engine.actions(unified)
      _ <- printTagged("remote state", remoteDb)
      _ <- printTagged("localState", localDb)
      _ <- printTagged("unified state", unified)
      _ <- printTagged("actions", actions)

      _ <- IO.whenA(cli.wetRun)(s3.playAll(actions))

    } yield ExitCode.Success
    res.recoverWith { case CliError(help) =>
      con.error(help).as(ExitCode.Error)
    }
  }
}
