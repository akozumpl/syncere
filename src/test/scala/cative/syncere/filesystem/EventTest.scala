package cative.syncere.filesystem

import java.nio.file.Path
import java.nio.file.WatchEvent
import java.nio.file.WatchEvent.Kind
import java.nio.file.{StandardWatchEventKinds => K}

import cats.data.Validated.Valid

import weaver.SimpleIOSuite

object EventTest extends SimpleIOSuite {

  val AKey = "convinced/enthusiasts"

  val event = new WatchEvent[Path] {
    override def kind(): Kind[Path] = K.ENTRY_MODIFY
    override def count(): Int = 1
    override def context(): Path =
      Path.of(AKey)
  }

  pureTest("Decodes a WatchService event.") {
    expect(
      Event.decodeWatchEvent(Path.of("/created/by"))(event) == Valid(
        Modification(AKey, Path.of("/created/by/convinced/enthusiasts"))
      )
    )
  }

}
