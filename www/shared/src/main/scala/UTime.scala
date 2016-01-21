package org.ukenergywatch.www

import scala.concurrent.duration.{ FiniteDuration, MILLISECONDS }

case class UInstant(millisFromEpoch: Long) extends AnyVal {
}

case class UDuration(millis: Long) extends AnyVal {
  def toConcurrent: FiniteDuration = FiniteDuration(millis, MILLISECONDS)
}

object UTimeExtensions {

  implicit class RichInt(val v: Int) extends AnyVal {
    def millis: UDuration = UDuration(v)
    def milli: UDuration = millis
  }

}
