package cative.syncere

import java.time.Instant

import cats.Show
import cats.data.Validated
import cats.effect.IO
import cats.effect.std.Console
import cats.syntax.show._

given Show[Instant] = Show.fromToString

given [A](using Show[A]): Show[List[A]] =
  Show.show(_.map(_.show).mkString("\n  ", "\n  ", ""))

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
