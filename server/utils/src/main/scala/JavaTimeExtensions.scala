package org.ukenergywatch.utils

import java.time.{ Clock => JavaClock }
import java.time.{ ZoneOffset, LocalDateTime, Duration, Instant, LocalDate }
import java.time.temporal.{ ChronoField, ChronoUnit }

object JavaTimeExtensions {

  object Clock {
    def utc: JavaClock = JavaClock.systemUTC()
  }

  implicit class RichJavaClock(val clock: JavaClock) extends AnyVal {
    def now(): LocalDateTime = LocalDateTime.now(clock)
  }

  implicit class RichInstant(val i: Instant) extends AnyVal {
    def toLocalDate: LocalDate = LocalDate.ofEpochDay(i.millis / (86400L * 1000L))
    def millis: Long = i.getEpochSecond * 1000L + i.getNano / 1000000L

    def +(d: Duration): Instant = i.plus(d)
    def -(d: Duration): Instant = i.minus(d)
    def -(j: Instant): Duration = Duration.between(j, i)

    def <(other: Instant): Boolean = i.isBefore(other)
    def <=(other: Instant): Boolean = !i.isAfter(other)
  }

  implicit class RichLocalDate(val ld: LocalDate) extends AnyVal {
    def toInstant: Instant = ld.toEpochDay.daysToInstant
  }

  implicit class RichDuration(val d: Duration) extends AnyVal {
    def millis: Long = d.getSeconds * 1000L + d.getNano / 1000000L
    def secondsDouble: Double = d.getSeconds.toDouble + d.getNano.toDouble * 1e-9

    def *(scale: Double): Duration = (d.secondsDouble * scale).seconds
    def /(other: Duration): Double = d.secondsDouble / other.secondsDouble
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
  }

}
