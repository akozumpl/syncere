package cative.syncere

import java.nio.file.Path
import java.time.Instant

import cative.syncere.filesystem.Md5
import cative.syncere.filesystem.Modification
import cative.syncere.meta.Local
import cative.syncere.meta.Remote

trait TestValues {
  val HourSeconds = 3600

  val key1 = "fileio.mkv"
  val key2 = "unbearable.json"
  val path1 = Path.of("/var/syncme", "fileio.mkv")

  val md5 = Md5.fromStringUnsafe("75d7a0c43d6e92130301bb29185e24f2")
  val changedMd5 = Md5.fromStringUnsafe("d4a01b26c18515fe71695a183a9dcfcb")

  val anInstant = Instant.parse("2030-12-03T10:15:30.00Z")

  val modification = Modification(key1, path1)

  val localIntel1 = Local(key1, md5, anInstant)
  val localIntel2 = Local(key2, md5, anInstant)
  val remoteIntel1 = Remote(key1, md5, anInstant)
  val remoteIntel2 = Remote(key2, md5, anInstant)
}
