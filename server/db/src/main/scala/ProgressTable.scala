package org.ukenergywatch.db

import slick.driver.JdbcDriver.api.MappedTo

case class ProgressType(val value: Byte) extends MappedTo[Byte]
object ProgressType {
  object ActualGeneration extends ProgressType(1)
  object PredictedGeneration extends ProgressType(2)
  object GenerationByFuelType extends ProgressType(3)
}

case class Progress(
  progressType: ProgressType,
  fromTime: DbTime,
  toTime: DbTime,
  id: Int = 0
) extends MergeableValue

trait ProgressTable extends Mergeable {
  val driver: slick.driver.JdbcDriver
  import driver.api._

  class Progresses(tag: Tag) extends Table[Progress](tag, "progress") with MergeableTable {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def progressType = column[ProgressType]("progressType")
    def fromTime = column[DbTime]("fromTime")
    def toTime = column[DbTime]("toTime")
    def * = (progressType, fromTime, toTime, id) <> (Progress.tupled, Progress.unapply)
  }

  object progresses extends TableQuery[Progresses](new Progresses(_)) with MergeQuery[Progress, Progresses] {
    protected def filter(item: Progress) = x => x.progressType === item.progressType
  }

}
