package cative.syncere

import java.time.Instant

import cats.Show
import cats.syntax.show._

given Show[Instant] = Show.fromToString

given [A](using Show[A]): Show[List[A]] =
  Show.show(_.map(_.show).mkString("\n  ", "\n  ", ""))
