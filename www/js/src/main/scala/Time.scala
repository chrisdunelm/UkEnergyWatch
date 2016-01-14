package org.ukenergywatch.www

import scala.scalajs.js
import scala.concurrent.duration.{ FiniteDuration, MILLISECONDS }

case class Instant(secondsFromEpoch: Double) extends AnyVal {
}

object Instant {
  def utcNow(): Instant = Instant(js.Date.now())
}

case class Duration(seconds: Double) extends AnyVal {
  def toConcurrent: FiniteDuration = FiniteDuration((seconds * 1000.0).toLong, MILLISECONDS)
}

object Duration {
}


object TimeExtensions {

  implicit class RichInt(val i: Int) extends AnyVal {
    def millis: Duration = Duration(i.toDouble * 1e-3)
    def milli: Duration = millis
    def seconds: Duration = Duration(i.toDouble)
    def second: Duration = seconds
    def minutes: Duration = Duration(i.toDouble * 60.0)
    def minute: Duration = minute
    def hours: Duration = Duration(i.toDouble * 3600.0)
    def hour: Duration = hours
  }

}
