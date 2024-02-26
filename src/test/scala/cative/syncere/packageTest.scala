package cative.syncere

import java.time.Instant

import weaver.SimpleIOSuite

object packageTest extends SimpleIOSuite {
  pureTest("Instant.getEpochMillis computes.") {
    val exact = Instant.ofEpochSecond(449672400L, 0)
    val almost = Instant.ofEpochSecond(449672400L, 1234)
    val nonzero = Instant.ofEpochSecond(449672400L, 123_456_789)

    expect(exact.getEpochMillis == 449672400000L) and expect(
      almost.getEpochMillis == 449672400000L
    ) and expect(
      nonzero.getEpochMillis == 449672400123L
    )
  }
}
