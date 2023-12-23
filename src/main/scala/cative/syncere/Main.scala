package cative.syncere

import java.nio.file.FileSystems
import java.nio.file.FileVisitor
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.WatchEvent
import java.nio.file.WatchService
import java.nio.file.{StandardWatchEventKinds => K}
import java.util.concurrent.TimeUnit

import scala.jdk.CollectionConverters._
import scala.jdk.StreamConverters._
import cats.Show
import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.syntax.show._

import cative.syncere.engine.Engine
import cative.syncere.filesystem.Md5
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
