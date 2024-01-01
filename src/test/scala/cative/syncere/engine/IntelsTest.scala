package cative.syncere.engine

import cats.syntax.show._

import weaver.SimpleIOSuite

import cative.syncere.TestValues

object IntelsTest extends SimpleIOSuite with TestValues {
  pureTest("Shows up with newlines.") {
    expect(
      Intels
        .fresh(List(localIntel1, localIntel2))
        .show
        .trim
        .split('\n')
        .length == 2
    )
  }
}
