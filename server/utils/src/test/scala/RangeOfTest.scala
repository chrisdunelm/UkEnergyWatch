package org.ukenergywatch.utils

import org.scalatest._
import org.ukenergywatch.utils.RangeOfExtensions._

class RangeOfTest extends FunSuite with Matchers {

  def s(from: Int, to: Int): SimpleRangeOf[Int] = SimpleRangeOf(from, to)

  test("intersect") {
    SimpleRangeOf(0, 10) intersect SimpleRangeOf(5, 15) shouldBe Some(SimpleRangeOf(5, 10))
    SimpleRangeOf(5, 15) intersect SimpleRangeOf(0, 10) shouldBe Some(SimpleRangeOf(5, 10))
    SimpleRangeOf(0, 15) intersect SimpleRangeOf(5, 10) shouldBe Some(SimpleRangeOf(5, 10))
    SimpleRangeOf(5, 10) intersect SimpleRangeOf(0, 15) shouldBe Some(SimpleRangeOf(5, 10))
    SimpleRangeOf(0, 5) intersect SimpleRangeOf(10, 15) shouldBe None
    SimpleRangeOf(0, 5) intersect SimpleRangeOf(5, 10) shouldBe None
    SimpleRangeOf(10, 15) intersect SimpleRangeOf(0, 5) shouldBe None
    SimpleRangeOf(5, 10) intersect SimpleRangeOf(0, 5) shouldBe None
  }

  test("union") {
    s(0, 10) union s(20, 30) shouldBe s(0, 30)
    s(0, 10) union s(2, 3) shouldBe s(0, 10)
    s(20, 30) union s(0, 10) shouldBe s(0, 30)
    s(2, 3) union s(0, 10) shouldBe s(0, 10)
    s(0, 10) union s(5, 15) shouldBe s(0, 15)
    s(5, 15) union s(0, 10) shouldBe s(0, 15)
  }

  test("range - a only") {
    (Seq(s(0, 1), s(5, 6)) - Seq.empty) shouldBe Seq(s(0, 1), s(5, 6))
  }

  test("range - b only") {
    (Seq.empty[RangeOf[Int]] - Seq(s(0, 1), s(5, 6))) shouldBe Seq.empty
  }

  test("range - identical") {
    (Seq(s(0, 1), s(2, 3)) - Seq(s(0, 1), s(2, 3))) shouldBe Seq.empty
  }

  test("range - 1") {
    val a = Seq(s(0, 10), s(20, 30))
    val b = Seq(s(5, 15), s(25, 35))
    (a - b) shouldBe Seq(s(0, 5), s(20, 25))
  }

  test("range - 2") {
    val a = Seq(s(5, 15), s(25, 35))
    val b = Seq(s(0, 10), s(20, 30))
    (a - b) shouldBe Seq(s(10, 15), s(30, 35))
  }

  test("range - internal") {
    (Seq(s(1, 2)) - Seq(s(0, 3))) shouldBe Seq.empty
    (Seq(s(0, 3)) - Seq(s(1, 2))) shouldBe Seq(s(0, 1), s(2, 3))
  }

  test("range - a long") {
    val a = Seq(s(0, 100))
    val b = Seq(s(0, 5), s(10, 15), s(20, 25), s(95,105))
    (a - b) shouldBe Seq(s(5, 10), s(15, 20), s(25, 95))
  }

  test("range - b long") {
    val a = Seq(s(0, 5), s(10, 15), s(20, 25), s(95, 105))
    val b = Seq(s(0, 100))
    (a - b) shouldBe Seq(s(100, 105))
  }

  test("coalesce") {
    Seq(s(0, 1), s(1, 2), s(3, 4)).coalesce shouldBe Seq(s(0, 2), s(3, 4))
  }

}
