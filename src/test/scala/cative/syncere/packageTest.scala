package cative.syncere

import java.time.Instant

import scala.concurrent.duration._
import cats.effect.Ref
import cats.syntax.monadError.*
import cats.syntax.option.*

import weaver.SimpleIOSuite

object packageTest extends SimpleIOSuite with TestValues {
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

  test("retry() can retry infinitely.") {
    val ten = Ref.unsafe(10)
    val io = ten
      .getAndUpdate(_ - 1)
      .map(_ < 0)
      .ensure(IoException)(identity)
    retry(None, io, 1.millisecond, None).map(res => expect(res))
  }

  test("retry() will retry given number of times.") {
    val four = Ref.unsafe(4)
    val io = four
      .getAndUpdate(_ - 1)
      .map(_ < 0)
      .ensure(IoException)(identity)
    for {
      notEnough <- retry(None, io, 1.millisecond, 4.some).attempt
      enough <- retry(None, io, 1.millisecond, 5.some).attempt
    } yield expect(enough.isRight && notEnough.isLeft)
  }
}
