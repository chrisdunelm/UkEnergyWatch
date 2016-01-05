package org.ukenergywatch.db

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import org.ukenergywatch.utils.{ RangeOf, SimpleRangeOf }
import scala.util.Try

trait MergeableValue extends RangeOf[Instant] {
  def id: Int
  def fromTime: DbTime
  def toTime: DbTime

  override def from: Instant = fromTime.toInstant
  override def to: Instant = toTime.toInstant
}

trait Mergeable {
  val driver: slick.driver.JdbcDriver
  import driver.api._

  trait MergeableTable {
    def id: Rep[Int]
    def fromTime: Rep[DbTime]
    def toTime: Rep[DbTime]
  }

  trait MergeQuery[TValue <: MergeableValue, TTable <: Table[TValue] with MergeableTable] {
    this: TableQuery[TTable] =>

    protected def mergeFilter(item: TValue): TTable => Rep[Boolean]

    protected def preSearch(fromTime: DbTime, toTime: DbTime): Query[TTable, TValue, Seq] = {
      this
    }

    protected def addItem(item: TValue): DBIO[_] = {
      this += item
    }

    protected def updateItem(id: Int, range: RangeOf[DbTime]): DBIO[_] = {
      this
        .filter(_.id === id)
        .map(x => (x.fromTime, x.toTime))
        .update((range.from, range.to))
    }

    def merge(item: TValue): DBIO[Try[Unit]] = {
      val qExisting = preSearch(item.fromTime, item.toTime)
        .filter(e => e.toTime >= item.fromTime && e.fromTime <= item.toTime)
        .filter(mergeFilter(item))
        .sortBy(_.fromTime)
        .take(3)
      val qUpdate = qExisting.result.flatMap { existings =>
        existings.toList match {
          case Nil =>
            // Nothing to merge with, just add
            addItem(item)
          case a :: Nil =>
            // Single mergeable neighbour, either before or after item
            if (a.toTime == item.fromTime) {
              updateItem(a.id, SimpleRangeOf(a.fromTime, item.toTime))
            } else if (a.fromTime == item.toTime) {
              updateItem(a.id, SimpleRangeOf(item.fromTime, a.toTime))
            } else {
              throw new Exception("Failed. Overlapping times")
            }
          case a :: b :: Nil =>
            if (a.toTime == item.fromTime && item.toTime == b.fromTime) {
              val update = updateItem(a.id, SimpleRangeOf(a.fromTime, b.toTime))
              val delete = this.filter(_.id === b.id).delete
              update >> delete
            } else {
              throw new Exception("Failed. Overlapping times")
            }
          case _ =>
            throw new Exception("Failed. Too many neighbours")
        }
      }
      val qTry = qUpdate >> DBIO.successful(()).asTry
      qTry.transactionally
    }
  }

}
