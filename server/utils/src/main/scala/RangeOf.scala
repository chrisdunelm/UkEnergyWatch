package org.ukenergywatch.utils

import scala.annotation.tailrec
import org.ukenergywatch.utils.maths._
import org.ukenergywatch.utils.maths.Implicits._

trait RangeOf[T] {
  def from: T
  def to: T

  def toSimple: SimpleRangeOf[T] = SimpleRangeOf(from, to)

  def atFraction[M](f: Double)(implicit a: Plus[T, M], b: Minus[T, M], c: Scale[M]): T = {
    from + ((to - from) scale f)
  }

  def overlaps(that: RangeOf[T])(implicit o: Ordering[T]): Boolean = {
    o.lt(this.from, that.to) && o.lt(that.from, this.to)
  }

  def intersect(that: RangeOf[T])(implicit o: Ordering[T]): Option[SimpleRangeOf[T]] = {
    if (this overlaps that) {
      val from = if (o.gt(this.from, that.from)) this.from else that.from
      val to = if (o.lt(this.to, that.to)) this.to else that.to
      Some(SimpleRangeOf[T](from, to))
    } else {
      None
    }
  }

  def union(that: RangeOf[T])(implicit o: Ordering[T]): SimpleRangeOf[T] = {
    val from = if (o.lt(this.from, that.from)) this.from else that.from
    val to = if (o.gt(this.to, that.to)) this.to else that.to
    SimpleRangeOf[T](from, to)
  }
}

trait RangeOfValue[T, Value] extends RangeOf[T] {
  def value0: Value
  def value1: Value

  def interpolatedValue[M, N](tValue: T)(
    implicit a: Minus[T, M], b: Minus[Value, N], c: Plus[Value, N], d: Scale[M], e: Scale[N]
  ): Value = {
    val p = (tValue - from) ratio (to - from)
    value0 + ((value1 - value0) scale p)
  }
}

case class SimpleRangeOf[T](from: T, to: T) extends RangeOf[T] {
  override def toSimple: SimpleRangeOf[T] = this
}

case class SimpleRangeOfValue[T, V](from: T, to: T, value0: V, value1: V) extends RangeOfValue[T, V]

object RangeOfExtensions {

  implicit class RichSeqMergeableValue[T : Ordering](a: Seq[RangeOf[T]]) {
    val o = implicitly[Ordering[T]]

    // Requires both inputs to be sorted and non-overlapping
    def -(b: Seq[RangeOf[T]]): Seq[SimpleRangeOf[T]] = {
      @tailrec def inner(as: List[RangeOf[T]], bs: List[RangeOf[T]], result: Vector[SimpleRangeOf[T]]):
          Seq[SimpleRangeOf[T]] = {
        (as, bs) match {
          case (a :: aTail, b :: bTail) =>
            if (a overlaps b) {
              if (o.lt(a.from, b.from)) {
                val r = SimpleRangeOf(a.from, b.from)
                inner(SimpleRangeOf(b.from, a.to) :: aTail, bs, result :+ r)
              } else if (o.lt(b.from, a.from)) {
                inner(as, SimpleRangeOf(a.from, b.to) :: bTail, result)
              } else {
                if (o.lt(a.to, b.to)) {
                  inner(aTail, SimpleRangeOf(a.to, b.to) :: bTail, result)
                } else if (o.lt(b.to, a.to)) {
                  inner(SimpleRangeOf(b.to, a.to) :: aTail, bTail, result)
                } else {
                  inner(aTail, bTail, result)
                }
              }
            } else {
              if (o.lt(a.from, b.from)) {
                inner(aTail, bs, result :+ a.toSimple)
              } else {
                inner(as, bTail, result)
              }
            }
          case (as, Nil) =>
            result ++ as.map(_.toSimple)
          case (Nil, _) =>
            result
        }
      }
      inner(a.toList, b.toList, Vector.empty).coalesce
    }

    def coalesce: Seq[SimpleRangeOf[T]] = {
      @tailrec def inner(rs: List[RangeOf[T]], result: Vector[SimpleRangeOf[T]]): Vector[SimpleRangeOf[T]] = {
        rs match {
          case a :: b :: tail if a.to == b.from =>
            inner(SimpleRangeOf(a.from, b.to) :: tail, result)
          case a :: tail =>
            inner(tail, result :+ a.toSimple)
          case Nil =>
            result
        }
      }
      inner(a.toList, Vector.empty)
    }

  }

}

object AlignedRangeOf {

  import org.ukenergywatch.utils.RangeOfExtensions._
  import java.time.Instant
  import org.ukenergywatch.utils.JavaTimeExtensions._

  private def simpleAlign(rs: Seq[RangeOf[Instant]], align: Long): Seq[SimpleRangeOf[Instant]] = {
    rs.coalesce.flatMap { r =>
      val baseTime = {
        val t0 = r.from.millis
        val rem = t0 % align
        val t1 = t0 - rem
        if (rem == 0) t1 else t1 + align
      }
      Seq.range(baseTime, r.to.millis - align + 1L, align).map { start =>
        SimpleRangeOf(start.millisToInstant, (start + align).millisToInstant)
      }
    }
  }

  // Get the hour-aligned ranges within the input ranges
  def hour(rs: Seq[RangeOf[Instant]]): Seq[SimpleRangeOf[Instant]] = {
    val millisPerHour = 1000L * 60L * 60L
    simpleAlign(rs, millisPerHour)
  }

  def day(rs: Seq[RangeOf[Instant]]): Seq[SimpleRangeOf[Instant]] = {
    val millisPerDay = 1000L * 60L * 60L * 24L
    simpleAlign(rs, millisPerDay)
  }

  def week(rs: Seq[RangeOf[Instant]]): Seq[SimpleRangeOf[Instant]] = {
    val millisPerWeek = 1000L * 60L * 60L * 24L * 7L
    simpleAlign(rs, millisPerWeek)
  }

  def month(rs: Seq[RangeOf[Instant]]): Seq[SimpleRangeOf[Instant]] = {
    rs.coalesce.flatMap { r =>
      val baseTime = r.from.toLocalDate.withDayOfMonth(1)
      val dts = Iterator.iterate(baseTime) { t => t.plusMonths(1) }
      val result = dts.map { date =>
        SimpleRangeOf(date.toInstant, date.plusMonths(1).toInstant)
      }
      result.dropWhile(_.from < r.from).takeWhile(_.to <= r.to)
    }
  }

  def year(rs: Seq[RangeOf[Instant]]): Seq[SimpleRangeOf[Instant]] = {
    rs.coalesce.flatMap { r =>
      val baseTime = r.from.toLocalDate.withDayOfYear(1)
      val dts = Iterator.iterate(baseTime) { t => t.plusYears(1) }
      val result = dts.map { date =>
        SimpleRangeOf(date.toInstant, date.plusYears(1).toInstant)
      }
      result.dropWhile(_.from < r.from).takeWhile(_.to <= r.to)
    }
  }

}
