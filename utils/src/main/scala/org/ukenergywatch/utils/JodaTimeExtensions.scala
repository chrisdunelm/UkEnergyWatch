package org.ukenergywatch.utils

import org.joda.time._

object JodaTimeExtensions {

  implicit class RichDateTime(val dt: DateTime) extends AnyVal {
    def -(d: ReadableDuration): DateTime = dt.minus(d)
  }

  implicit class RichReadableInstant(val i: ReadableInstant) extends AnyVal {
    def minute: Int = i.get(DateTimeFieldType.minuteOfHour)
    def hour: Int = i.get(DateTimeFieldType.hourOfDay)
    def day: Int = i.get(DateTimeFieldType.dayOfMonth)
    def month: Int = i.get(DateTimeFieldType.monthOfYear)
    def year: Int = i.get(DateTimeFieldType.year)

    def totalSeconds: Int = (i.getMillis / 1000L).toInt

    def +(d: ReadableDuration): ReadableInstant = i.toInstant.plus(d)

    def >(other: ReadableInstant): Boolean = i.isAfter(other)
    def <(other: ReadableInstant): Boolean = i.isBefore(other)
    def >=(other: ReadableInstant): Boolean = !(i < other)
    def <=(other: ReadableInstant): Boolean = !(i > other)
  }

  implicit class RichInt(val i: Int) extends AnyVal {
    def seconds: ReadableDuration = Duration.standardSeconds(i)
    def second = seconds
    def minutes: ReadableDuration = Duration.standardMinutes(i)
    def minute = minutes
    def hours: ReadableDuration = Duration.standardHours(i)
    def hour = hours

    def toInstant: ReadableInstant = new Instant(i.toLong * 1000L)
  }

}
