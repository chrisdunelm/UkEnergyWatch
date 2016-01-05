package org.ukenergywatch.utils

import org.scalatest._
import org.ukenergywatch.utils.RangeOfExtensions._
import java.time.Instant
import org.ukenergywatch.utils.JavaTimeExtensions._

class AlignedRangeOfTest extends FunSuite with Matchers {

  def s(fromMinute: Int, toMinute: Int): SimpleRangeOf[Instant] =
    SimpleRangeOf(fromMinute.minutesToInstant, toMinute.minutesToInstant)

  def d(fromDay: Int, toDay: Int): SimpleRangeOf[Instant] =
    SimpleRangeOf(fromDay.daysToInstant, toDay.daysToInstant)

  def h(fromHour: Int, toHour: Int): SimpleRangeOf[Instant] =
    SimpleRangeOf(fromHour.hoursToInstant, toHour.hoursToInstant)

  test("hour") {
    AlignedRangeOf.hour(Seq(s(0, 60))) shouldBe Seq(s(0, 60))
    AlignedRangeOf.hour(Seq(s(0, 120))) shouldBe Seq(s(0, 60), s(60, 120))
    AlignedRangeOf.hour(Seq(s(1, 61))) shouldBe Seq.empty
    AlignedRangeOf.hour(Seq(s(1, 121))) shouldBe Seq(s(60, 120))
  }

  test("day") {
    AlignedRangeOf.day(Seq(d(0, 1))) shouldBe Seq(d(0, 1))
    AlignedRangeOf.day(Seq(d(0, 2))) shouldBe Seq(d(0, 1), d(1, 2))
    AlignedRangeOf.day(Seq(h(1, 25))) shouldBe Seq.empty
    AlignedRangeOf.day(Seq(h(1, 49))) shouldBe Seq(d(1, 2))
  }

  test("week") {
    AlignedRangeOf.week(Seq(d(0, 7))) shouldBe Seq(d(0, 7))
    AlignedRangeOf.week(Seq(d(0, 14))) shouldBe Seq(d(0, 7), d(7, 14))
    AlignedRangeOf.week(Seq(d(1, 8))) shouldBe Seq.empty
    AlignedRangeOf.week(Seq(d(1, 15))) shouldBe Seq(d(7, 14))
  }

  test("month") {
    AlignedRangeOf.month(Seq(d(0, 31))) shouldBe Seq(d(0, 31))
    AlignedRangeOf.month(Seq(d(0, 31 + 28))) shouldBe Seq(d(0, 31), d(31, 31 + 28))
    AlignedRangeOf.month(Seq(d(1, 50))) shouldBe Seq.empty
    AlignedRangeOf.month(Seq(d(1, 70))) shouldBe Seq(d(31, 31 + 28))
  }

  test("year") {
    AlignedRangeOf.year(Seq(d(0, 365))) shouldBe Seq(d(0, 365))
    AlignedRangeOf.year(Seq(d(0, 365 + 365))) shouldBe Seq(d(0, 365), d(365, 365 + 365))
    AlignedRangeOf.year(Seq(d(0, 365 + 365 + 366))) shouldBe
      Seq(d(0, 365), d(365, 365 + 365), d(365 + 365, 365 + 365 + 366))
    AlignedRangeOf.year(Seq(d(1, 500))) shouldBe Seq.empty
    AlignedRangeOf.year(Seq(d(1, 800))) shouldBe Seq(d(365, 365 + 365))
  }

}
