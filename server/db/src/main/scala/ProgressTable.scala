package org.ukenergywatch.db

case class Progress(
  rawDataType: RawDataType,
  fromTime: DbTime,
  toTime: DbTime,
  id: Int = 0
) extends MergeableValue {
  def id0: Progress = copy(id = 0)
}

trait ProgressTable extends Mergeable {
  val driver: slick.driver.JdbcDriver
  import driver.api._

  class Progresses(tag: Tag) extends Table[Progress](tag, "progress") with MergeableTable {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def rawDataType = column[RawDataType]("rawDataType")
    def fromTime = column[DbTime]("fromTime")
    def toTime = column[DbTime]("toTime")
    def * = (rawDataType, fromTime, toTime, id) <> (Progress.tupled, Progress.unapply)
  }

  object progresses extends TableQuery[Progresses](new Progresses(_)) with MergeQuery[Progress, Progresses] {
    protected def mergeFilter(item: Progress) = x => x.rawDataType === item.rawDataType
  }

}
