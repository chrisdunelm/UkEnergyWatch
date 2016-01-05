package org.ukenergywatch.utils

import org.scalatest._
import org.ukenergywatch.utils.JavaTimeExtensions._
import org.ukenergywatch.utils.maths.Implicits._

class RangeOfValueTest extends FunSuite with Matchers {

  test("interpolatedValue") {
    val r = SimpleRangeOfValue(0.0, 1.0, 0.0, 1.0)
    r.interpolatedValue(0.0) shouldBe 0.0 +- 1e-10
    r.interpolatedValue(1.0) shouldBe 1.0 +- 1e-10
    r.interpolatedValue(0.5) shouldBe 0.5 +- 1e-10

    val i = SimpleRangeOfValue(0.secondsToInstant, 1.secondsToInstant, 0.0, 1.0)
    i.interpolatedValue(0.secondsToInstant) shouldBe 0.0 +- 1e-10
    i.interpolatedValue(1.secondsToInstant) shouldBe 1.0 +- 1e-10
    i.interpolatedValue(500.millisToInstant) shouldBe 0.5 +- 1e-10
  }

}
