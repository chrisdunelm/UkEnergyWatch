package org.ukenergywatch.utils

import org.scalatest._
import java.time.{ LocalDate, OffsetDateTime, ZoneOffset }
import org.ukenergywatch.utils.JavaTimeExtensions._

class JavaTimeExtensionsTest extends FunSuite with Matchers {

  def ld(month: Int, day: Int) = LocalDate.of(2015, month, day)
  def odt(month: Int, day: Int, hour: Int, minute: Int, z: ZoneOffset) =
    OffsetDateTime.of(2015, month, day, hour, minute, 0, 0, z)

  test("LocalDate.withSettlementPeriod") {
    // Normal 24 hour day, GMT (winter time)
    ld(1, 1).atStartOfSettlementPeriod(1) shouldBe odt(1, 1, 0, 0, ZoneOffset.ofHours(0))
    // Normal 24 hour day, GMT+1 (summer time)
    ld(7, 1).atStartOfSettlementPeriod(1) shouldBe odt(7, 1, 0, 0, ZoneOffset.ofHours(1))
    // Day of winter->summer time, clocks go forward, only 23 hours in day
    // Clock-change happens at 1am, so 1:00 - 2:00 is skipped
    ld(3, 29).atStartOfSettlementPeriod(1) shouldBe odt(3, 29, 0, 0, ZoneOffset.ofHours(0))
    ld(3, 29).atStartOfSettlementPeriod(2) shouldBe odt(3, 29, 0, 30, ZoneOffset.ofHours(0))
    ld(3, 29).atStartOfSettlementPeriod(3) shouldBe odt(3, 29, 2, 0, ZoneOffset.ofHours(1))
    ld(3, 29).atStartOfSettlementPeriod(46) shouldBe odt(3, 29, 23, 30, ZoneOffset.ofHours(1))
    // Day of summer->winter time, clocks go backwards, 25 hours in day
    // Clock-change happens at 2am, 1:00 - 2:00 is repeated
    ld(10, 25).atStartOfSettlementPeriod(1) shouldBe odt(10, 25, 0, 0, ZoneOffset.ofHours(1))
    ld(10, 25).atStartOfSettlementPeriod(2) shouldBe odt(10, 25, 0, 30, ZoneOffset.ofHours(1))
    ld(10, 25).atStartOfSettlementPeriod(3) shouldBe odt(10, 25, 1, 0, ZoneOffset.ofHours(1))
    ld(10, 25).atStartOfSettlementPeriod(4) shouldBe odt(10, 25, 1, 30, ZoneOffset.ofHours(1))
    ld(10, 25).atStartOfSettlementPeriod(5) shouldBe odt(10, 25, 1, 0, ZoneOffset.ofHours(0))
    ld(10, 25).atStartOfSettlementPeriod(50) shouldBe odt(10, 25, 23, 30, ZoneOffset.ofHours(0))
  }

  test("Instant settlementDate/Period") {
    // TODO
  }

}
