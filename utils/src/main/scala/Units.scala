package org.ukenergywatch.utils.units

import java.time.Duration
import org.ukenergywatch.utils.JavaTimeExtensions._
import org.ukenergywatch.utils.maths._

case class Energy private(joules: Double) extends AnyVal {
  def +(that: Energy): Energy = Energy(this.joules + that.joules)
  def /(duration: Duration): Power = Power.watts(joules / duration.secondsDouble)
}

object Energy {
  def joules(joules: Double) = Energy(joules)

  implicit val ordering = Ordering.by[Energy, Double](_.joules)
}

case class Power private(watts: Double) extends AnyVal {
  def +(that: Power): Power = Power(this.watts + that.watts)
  def -(that: Power): Power = Power(this.watts - that.watts)
  def *(scale: Double): Power = Power(watts * scale)
  def *(duration: Duration): Energy = Energy.joules(watts * duration.secondsDouble)
  def /(that: Power): Double = this.watts / that.watts
}

object Power {
  val zero = Power(0)
  def watts(watts: Double) = Power(watts)
  def kiloWatts(kiloWatts: Double) = Power(kiloWatts * 1e3)
  def megaWatts(megaWatts: Double) = Power(megaWatts * 1e6)

  implicit val ordering = Ordering.by[Power, Double](_.watts)

  implicit val maths = new Plus[Power, Power] with Minus[Power, Power] with Scale[Power] {
    def plus(a: Power, b: Power): Power = a + b
    def minus(a: Power, b: Power): Power = a - b
    def ratio(a: Power, b: Power): Double = a / b
    def scale(a: Power, b: Double): Power = a * b
  }
}
