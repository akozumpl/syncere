package cative.syncere

import java.io.IOException
import java.time.Instant

import scala.concurrent.duration._
import cats.effect.Ref
import cats.syntax.monadError.*

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

  test("retryInfinitely does just that.") {
    val thrice = Ref.unsafe(3)
    val io = thrice
      .getAndUpdate(_ - 1)
      .map(_ < 0)
      .ensure(new IOException("the aura of nostalgia"))(identity)
    retryInfinitely(None, io, 1.millisecond).map(res => expect(res))
  }
}
