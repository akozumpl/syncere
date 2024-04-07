package cative.syncere

import java.io.IOException
import java.time.Instant

import scala.concurrent.duration.FiniteDuration
import cats.Show
import cats.data.Validated
import cats.effect.IO
import cats.effect.std.Console
import cats.syntax.show._

import software.amazon.awssdk.core.exception.SdkClientException

given Show[Instant] = Show.fromToString

given [A](using Show[A]): Show[List[A]] =
  Show.show(_.map(_.show).mkString("\n  ", "\n  ", ""))

extension (i: Instant) {
  def getEpochMillis: Long = i.getEpochSecond() * 1000 + i.getNano() / 1000_000
}

extension [A](validated: Validated[Throwable, A]) {
  def toIO: IO[A] = IO.fromEither(validated.toEither)
}

def error[A: Show](a: A)(implicit con: Console[IO]): IO[Unit] = con.errorln(a)

def errorln[A](a: A)(implicit
    con: Console[IO],
    S: Show[A] = Show.fromToString[A]
): IO[Unit] = con.errorln(a)

def printShow[A: Show](a: A)(implicit con: Console[IO]): IO[Unit] =
  con.println(a)

def printTagged[A: Show](tag: String, a: A)(implicit
    con: Console[IO]
): IO[Unit] =
  con.println(s"--- $tag: ---") >> printShow(a)

/** Retries IO if it failed.
  *
  * `attempts` indicates maximum number of attempts to retry. `None` retries
  * indefinitelyu.
  */
def retry[A](
    doingWhat: Option[String],
    io: IO[A],
    delay: FiniteDuration,
    attempts: Option[Int]
): IO[A] =
  io.recoverWith { case e: (IOException | SdkClientException) =>
    attempts match {
      case Some(attempts) if attempts < 1 =>
        IO.raiseError(e)
      case attempts =>
        (doingWhat match {
          case Some(msg) => errorln(s"Failed $msg: ${e.getMessage()}")
          case None      => IO.unit
        }) *> IO.sleep(delay)
          *> retry(doingWhat, io, delay, attempts.map(_ - 1))
    }
  }
