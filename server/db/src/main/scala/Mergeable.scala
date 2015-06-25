package org.ukenergywatch.db

import scala.concurrent.ExecutionContext.Implicits.global

trait MergeableValue {
  def id: Int
  def fromTime: DbTime
  def toTime: DbTime
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

    protected def filter(item: TValue): TTable => Rep[Boolean]

    def merge(item: TValue): DBIOAction[scala.util.Try[Unit], NoStream, _] = {
      val qExisting = this
        .filter(e => e.toTime >= item.fromTime && e.fromTime <= item.toTime)
        .filter(filter(item))
        .sortBy(_.fromTime)
        .take(3)
      //println(qExisting.result.statements)
      val qUpdate = qExisting.result.flatMap { existings =>
        existings.toList match {
          case Nil =>
            // Nothing to merge with, just add
            this += item
          case a :: Nil =>
            // Single mergeable neighbour, either before or after item
            if (a.toTime == item.fromTime) {
              this.filter(_.id === a.id).map(_.toTime).update(item.toTime)
            } else if (a.fromTime == item.toTime) {
              this.filter(_.id === a.id).map(_.fromTime).update(item.fromTime)
            } else {
              throw new Exception("Failed. Overlapping times")
            }
          case a :: b :: Nil =>
            if (a.toTime == item.fromTime && item.toTime == b.fromTime) {
              val update = this.filter(_.id === a.id).map(_.toTime).update(b.toTime)
              val delete = this.filter(_.id === b.id).delete
              update andThen delete
            } else {
              throw new Exception("Failed. Overlapping times")
            }
          case _ =>
            throw new Exception("Failed. Too many neighbours")
        }
      }
      val qTry = qUpdate.andThen(DBIO.successful(())).asTry
      qTry.transactionally
    }
  }

}
