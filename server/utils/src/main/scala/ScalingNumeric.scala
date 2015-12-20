package org.ukenergywatch.utils.maths

import java.time.{ Instant, Duration }
import org.ukenergywatch.utils.JavaTimeExtensions._

trait Plus[A, B] {
  def plus(x: A, y: B): A
}

trait Minus[A, B] {
  def minus(x: A, y: A): B
}

trait Scale[A] {
  def ratio(a: A, b: A): Double
  def scale(a: A, b: Double): A
}

object Implicits {

  implicit class RichPlus[A](val l: A) {
    def +[B](r: B)(implicit op: Plus[A, B]): A = op.plus(l, r)
  }

  implicit class RichMinus[A](val l: A) {
    def -[B](r: A)(implicit op: Minus[A, B]): B = op.minus(l, r)
  }

  implicit class RichSCale[A](val l: A) {
    def ratio(r: A)(implicit op: Scale[A]): Double = op.ratio(l, r)
    def scale(r: Double)(implicit op: Scale[A]): A = op.scale(l, r)
  }

  implicit val plusDouble = new Plus[Double, Double] {
    def plus(x: Double, y: Double): Double = x + y
  }

  implicit val minusDouble = new Minus[Double, Double] {
    def minus(x: Double, y: Double): Double = x - y
  }

  implicit val scaleDouble = new Scale[Double] {
    def ratio(a: Double, b: Double): Double = a / b
    def scale(a: Double, b: Double): Double = a * b
  }

  implicit val plusInstant = new Plus[Instant, Duration] {
    def plus(x: Instant, y: Duration): Instant = x + y
  }

  implicit val minusInstant = new Minus[Instant, Duration] {
    def minus(x: Instant, y: Instant): Duration = x - y
  }

  implicit val scaleDuration = new Scale[Duration] {
    def ratio(a: Duration, b: Duration): Double = a / b
    def scale(a: Duration, b: Double): Duration = a * b
  }

}
