package cative.syncere.engine

import cats.syntax.show.*

import weaver.SimpleIOSuite

import cative.syncere.TestValues
import cative.syncere.meta.Local

object IntelsTest extends SimpleIOSuite with TestValues {
  val intels = Intels
    .fresh(List(localIntel1, localIntel2))

  pureTest("Compares as expected.") {
    expect(intels == intels) and
      expect(intels == intels.copy()) and
      expect(intels != Intels.empty) and
      expect(
        Intels.fresh(List(localIntel1)) != Intels.fresh(List(localIntel2))
      ) and
      expect(
        Intels.fresh(List(Local(key1, md5, anInstant))) != Intels.fresh(
          List(Local(key1, md5, anInstant.plusSeconds(1)))
        )
      )
  }

  pureTest("Shows up with newlines.") {
    expect(
      intels.show.trim
        .split('\n')
        .length == 2
    )
  }

  pureTest("updateOrElse() handles present key.") {
    expect(
      intels
        .updateOrElse(key1)(_ => remoteIntel1)(remoteIntel2)
        .intels(key1)
        == remoteIntel1
    )

  }

  pureTest("updateOrElse() handles removed key.") {
    val intels = Intels.fresh(List(localIntel2))
    expect(
      intels
        .updateOrElse(key1)(_ => remoteIntel1)(remoteIntel2)
        .intels(key1)
        == remoteIntel2
    )
  }

  pureTest("updateWith() can drop a key") {
    expect(
      intels.updateWith(key1)(_ => None) == Intels.fresh(List(localIntel2))
    )
  }

}
