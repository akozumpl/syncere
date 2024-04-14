package cative.syncere.engine

import weaver.SimpleIOSuite

import cative.syncere.TestValues
import cative.syncere.meta.*

object EngineTest extends SimpleIOSuite with TestValues {
  val aFullIntel = Intels.fresh(
    List(
      localIntel1,
      remoteIntel1
    )
  )

  // action tests
  pureTest("Local deletion on its own produces no action.") {
    expect(Engine.action(LocallyDeleted(key1, anInstant)) == NoOp)
  }

  // updateRemoteSnapshot() tests

  pureTest("Key missing from the RemoteSnapshot casuses deletion.") {
    expect(
      aFullIntel.absorb(RemoteSnapshot.empty) == Intels(
        Map(key1 -> FullRemotelyDeleted(localIntel1, RemotelyDeleted(key1)))
      )
    )
  }

  pureTest("Unchanged RemoteSnapshot cause no changes") {
    expect(aFullIntel.absorb(RemoteSnapshot(List(remoteIntel1))) == aFullIntel)
  }

  pureTest("A new remote key appearing is noted.") {
    expect(
      aFullIntel.absorb(RemoteSnapshot(List(remoteIntel1, remoteIntel2))) ==
        Intels(
          Map(
            key1 -> Full(localIntel1, remoteIntel1),
            key2 -> remoteIntel2
          )
        )
    )
  }

  pureTest("Remote deletion after local deletion drops the intel entirely.") {
    val intels = Intels(Map(key1 -> LocallyDeleted(key1, anInstant)))
    expect(intels.absorb(RemoteSnapshot.empty) == Intels.empty)
  }

}
