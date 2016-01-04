package org.ukenergywatch.utils

import java.time.{ Clock => JavaClock }
import java.time.{ ZoneOffset, LocalDateTime, Duration, Instant, LocalDate, OffsetDateTime, ZoneId, LocalTime }
import java.time.{ ZonedDateTime }
import java.time.temporal.{ ChronoField, ChronoUnit }
import scala.concurrent.duration.{ Duration => ScalaDuration }

case class Settlement(date: LocalDate, period: Int)

object JavaTimeExtensions {

  val londonZoneId: ZoneId = ZoneId.of("Europe/London")

  object Clock {
    def utc: JavaClock = JavaClock.systemUTC()
  }

  implicit class RichJavaClock(val clock: JavaClock) extends AnyVal {
    def now(): LocalDateTime = LocalDateTime.now(clock)
  }

  implicit class RichInstant(val i: Instant) extends AnyVal {
    def toLocalDate: LocalDate = LocalDate.ofEpochDay(i.millis / (86400L * 1000L))
    def toLocalDateTimeUtc: LocalDateTime =
      LocalDateTime.ofEpochSecond(i.getEpochSecond, i.getNano, ZoneOffset.UTC)
    def millis: Long = i.getEpochSecond * 1000L + i.getNano / 1000000L

    def +(d: Duration): Instant = i.plus(d)
    def -(d: Duration): Instant = i.minus(d)
    def -(j: Instant): Duration = Duration.between(j, i)

    def <(other: Instant): Boolean = i.isBefore(other)
    def <=(other: Instant): Boolean = !i.isAfter(other)
    def >(other: Instant): Boolean = i.isAfter(other)
    def >=(other: Instant): Boolean = !i.isBefore(other)

    def alignTo(duration: Duration): Instant = {
      val m = i.millis
      (m - m % duration.millis).millisToInstant
    }

    def settlement: Settlement = {
      val zdt = ZonedDateTime.ofInstant(i, londonZoneId)
      val ld = zdt.toLocalDate
      val zdtT0 = ZonedDateTime.of(ld, LocalTime.MIDNIGHT, londonZoneId)
      Settlement(
        date = ld,
        period = ((i - zdtT0.toInstant).millis / (30L * 60L * 1000L)).toInt + 1
      )
    }
  }

  implicit class RichLocalDate(val ld: LocalDate) extends AnyVal {
    def +(time: LocalTime): LocalDateTime = ld.atTime(time)

    def toInstant: Instant = ld.toEpochDay.daysToInstant

    def atStartOfSettlementPeriod(settlementPeriod: Int): OffsetDateTime = {
      val zonedDt = ld.atStartOfDay(londonZoneId)
      zonedDt.plusMinutes((settlementPeriod - 1) * 30).toOffsetDateTime
    }
    def atEndOfSettlementPeriod(settlementPeriod: Int): OffsetDateTime =
      atStartOfSettlementPeriod(settlementPeriod + 1)
  }

  implicit class RichLocalDateTime(val ldt: LocalDateTime) extends AnyVal {
    def toInstantUtc: Instant = ldt.atOffset(ZoneOffset.UTC).toInstant
  }

  implicit class RichDuration(val d: Duration) extends AnyVal {
    def millis: Long = d.getSeconds * 1000L + d.getNano / 1000000L
    def seconds: Long = d.getSeconds
    def secondsDouble: Double = d.getSeconds.toDouble + d.getNano.toDouble * 1e-9

    def *(scale: Double): Duration = (d.secondsDouble * scale).seconds
    def /(other: Duration): Double = d.secondsDouble / other.secondsDouble

    def <(other: Duration): Boolean = d.compareTo(other) < 0
    def <=(other: Duration): Boolean = d.compareTo(other) <= 0
    def >(other: Duration): Boolean = d.compareTo(other) > 0
    def >=(other: Duration): Boolean = d.compareTo(other) >= 0

    def toConcurrent: ScalaDuration = ScalaDuration.fromNanos(d.toNanos)
  }

  implicit class RichInt(val i: Int) extends AnyVal {
    def millisToInstant: Instant = Instant.ofEpochMilli(i)
    def secondsToInstant: Instant = Instant.ofEpochSecond(i)
    def minutesToInstant: Instant = Instant.ofEpochSecond(i.toLong * 60L)
    def hoursToInstant: Instant = Instant.ofEpochSecond(i.toLong * 3600L)
    def daysToInstant: Instant = Instant.ofEpochSecond(i.toLong * 86400L)

    def millis: Duration = Duration.ofMillis(i)
    def milli: Duration = millis
    def seconds: Duration = Duration.ofSeconds(i)
    def second: Duration = seconds
    def minutes: Duration = Duration.ofMinutes(i)
    def minute: Duration = minutes
    def hours: Duration = Duration.ofHours(i)
    def hour: Duration = hours
  }

  implicit class RichLong(val l: Long) extends AnyVal {
    def millisToInstant: Instant = Instant.ofEpochMilli(l)
    def secondsToInstant: Instant = Instant.ofEpochSecond(l)
    def minutesToInstant: Instant = Instant.ofEpochSecond(l * 60L)
    def hoursToInstant: Instant = Instant.ofEpochSecond(l * 3600L)
    def daysToInstant: Instant = Instant.ofEpochSecond(l * 86400L)
  }

  implicit class RichDouble(val d: Double) extends AnyVal {
    def seconds: Duration = Duration.ofNanos((d * 1e9).toLong)
    def minutes: Duration = (d * 60.0).seconds
  }

  // 2015-12-01
  val localDateRx = """(\d{4})-(\d{2})-(\d{2})""".r
  // 00:00:00
  val localTimeRx = """(\d{2}):(\d{2}):(\d{2})""".r
  // 2015-12-01 00:05:00
  val localDateTimeRx = """(\d{4})-(\d{2})-(\d{2}) (\d{2}):(\d{2}):(\d{2})""".r
  // Slightly strange name required for some reason
  implicit class RichStringForTimes(val s: String) extends AnyVal {
    def toLocalDate: LocalDate = s match {
      case localDateRx(year, month, day) => LocalDate.of(year.toInt, month.toInt, day.toInt)
      case _ => throw new Exception(s"Invalid LocalDate string: '$s'")
    }
    def toLocalTime: LocalTime = s match {
      case localTimeRx(hour, minute, second) => LocalTime.of(hour.toInt, minute.toInt, second.toInt)
      case _ => throw new Exception(s"Invalid LocalTime string: '$s'")
    }
    def toLocalDateTime: LocalDateTime = s match {
      case localDateTimeRx(year, month, day, hour, minute, second) =>
        LocalDateTime.of(year.toInt, month.toInt, day.toInt, hour.toInt, minute.toInt, second.toInt)
      case _ => throw new Exception(s"Invalid LocalDateTime string: '$s'")
    }
  }

}
