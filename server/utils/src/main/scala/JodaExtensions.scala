/*package org.ukenergywatch.utils

import org.joda.time._

object JodaExtensions {

  implicit class RichInt(val i: Int) extends AnyVal {
    def minutes: Duration = new Duration(i.toLong * 1000L * 60L)
    def minute = minutes
    def seconds: Duration = new Duration(i.toLong * 1000L)
    def second = seconds
    def millis: Duration = new Duration(i.toLong)
    def milli = millis
  }

  implicit class RichLong(val l: Long) extends AnyVal {
    def toInstant: Instant = new Instant(l)
  }

  implicit class RichReadableInstant(val i: ReadableInstant) extends AnyVal {
    def millis: Long = i.getMillis
    def seconds: Int = (i.getMillis / 1000L).toInt
    def + (d: ReadableDuration): Instant = i.toInstant.plus(d)
    def - (j: ReadableInstant): Duration = new Duration(j, i)
    def < (j: ReadableInstant): Boolean = i.isBefore(j)
    def <= (j: ReadableInstant): Boolean = !i.isAfter(j)
  }

  implicit class RichReadableDuration(val d: ReadableDuration) extends AnyVal {
    def millis: Long = d.getMillis
    def > (o: ReadableDuration): Boolean = d.isLongerThan(o)
  }

  implicit class RichDateTime(val dt: DateTime) extends AnyVal {
    def + (d: ReadableDuration): DateTime = dt.plus(d)
    def - (d: ReadableDuration): DateTime = dt.minus(d)
  }

}
 */
