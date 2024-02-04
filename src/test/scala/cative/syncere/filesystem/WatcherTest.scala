package cative.syncere.filesystem

import cats.syntax.validated.*

import weaver.SimpleIOSuite

import cative.syncere.TestValues

object WatcherTest extends SimpleIOSuite with TestValues {

  test("Strips invalid events.") {
    val ves = List(
      WatcherError("it is fine").invalid,
      modification.valid,
      WatcherError("it is fine").invalid
    )
    Watcher.stripInvalid(ves).map { res =>
      expect(res == List(modification))
    }
  }

}
