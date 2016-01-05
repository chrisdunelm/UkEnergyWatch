package org.ukenergywatch.utils

import org.scalatest._
import java.time.{ LocalDate, OffsetDateTime, ZoneOffset, LocalDateTime, ZoneId, ZonedDateTime }
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

  val londonZoneId = ZoneId.of("Europe/London")
  def zdtLondon(month: Int, day: Int, hour: Int, minute: Int) =
    ZonedDateTime.of(LocalDateTime.of(2015, month, day, hour, minute, 0), londonZoneId)

  test("Instant settlementDate/Period") {
    // Normal 24 hour day, GMT (winter time), 00:00
    zdtLondon(1, 1, 0, 0).toInstant.settlement shouldBe Settlement(ld(1, 1), 1)
    // Normal 24 hour day, GMT (winter time), 23:59
    zdtLondon(1, 1, 23, 59).toInstant.settlement shouldBe Settlement(ld(1, 1), 48)
    // Normal 24 hour day, GMT+1 (summer time), 00:00
    zdtLondon(7, 1, 0, 0).toInstant.settlement shouldBe Settlement(ld(7, 1), 1)
    // Normal 24 hour day, GMT+1 (summer time), 23:59
    zdtLondon(7, 1, 23, 59).toInstant.settlement shouldBe Settlement(ld(7, 1), 48)
    // Day of winter->summer time, clocks go forward , only 23 hours in day
    // Clock-change happens at 1am, so 1:00 - 2:00 is skipped
    zdtLondon(3, 29, 0, 0).toInstant.settlement shouldBe Settlement(ld(3, 29), 1)
    zdtLondon(3, 29, 0, 30).toInstant.settlement shouldBe Settlement(ld(3, 29), 2)
    zdtLondon(3, 29, 1, 0).toInstant.settlement shouldBe Settlement(ld(3, 29), 3)
    zdtLondon(3, 29, 1, 30).toInstant.settlement shouldBe Settlement(ld(3, 29), 4)
    zdtLondon(3, 29, 2, 0).toInstant.settlement shouldBe Settlement(ld(3, 29), 3)
    zdtLondon(3, 29, 2, 30).toInstant.settlement shouldBe Settlement(ld(3, 29), 4)
    zdtLondon(3, 29, 3, 0).toInstant.settlement shouldBe Settlement(ld(3, 29), 5)
    zdtLondon(3, 29, 23, 59).toInstant.settlement shouldBe Settlement(ld(3, 29), 46)
    // Day of summer->winter time, clocks go backwards, 25 hours in day
    // Clock-change happens at 2am, 1:00 - 2:00 is repeated
    zdtLondon(10, 25, 0, 0).toInstant.settlement shouldBe Settlement(ld(10, 25), 1)
    zdtLondon(10, 25, 0, 30).toInstant.settlement shouldBe Settlement(ld(10, 25), 2)
    zdtLondon(10, 25, 1, 0).toInstant.settlement shouldBe Settlement(ld(10, 25), 3)
    zdtLondon(10, 25, 1, 30).toInstant.settlement shouldBe Settlement(ld(10, 25), 4)
    zdtLondon(10, 25, 2, 0).toInstant.settlement shouldBe Settlement(ld(10, 25), 7)
    zdtLondon(10, 25, 2, 30).toInstant.settlement shouldBe Settlement(ld(10, 25), 8)
    zdtLondon(10, 25, 23, 59).toInstant.settlement shouldBe Settlement(ld(10, 25), 50)
  }

}
