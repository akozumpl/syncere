package cative.syncere.filesystem

import java.nio.file.Path
import java.nio.file.WatchEvent
import java.nio.file.WatchEvent.Kind
import java.nio.file.{StandardWatchEventKinds => K}

import cats.data.Validated.Valid

import weaver.SimpleIOSuite

object EventTest extends SimpleIOSuite {

  val path = Path.of("/convinced/enthusiasts")
  val event = new WatchEvent[Path] {
    override def kind(): Kind[Path] = K.ENTRY_MODIFY
    override def count(): Int = 1
    override def context(): Path = path
  }

  pureTest("Decodes a WatchService event.") {
    expect(Event.decodeWatchEvent(event) == Valid(Modification(path)))
  }

}
