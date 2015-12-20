package org.ukenergywatch.db

case class RawProgress(
  rawDataType: RawDataType,
  fromTime: DbTime,
  toTime: DbTime,
  id: Int = 0
) extends MergeableValue {
  def id0: RawProgress = copy(id = 0)
}

trait RawProgressTable extends Mergeable {
  val driver: slick.driver.JdbcDriver
  import driver.api._

  class RawProgresses(tag: Tag) extends Table[RawProgress](tag, "rawprogress") with MergeableTable {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def rawDataType = column[RawDataType]("rawDataType")
    def fromTime = column[DbTime]("fromTime")
    def toTime = column[DbTime]("toTime")
    def * = (rawDataType, fromTime, toTime, id) <> (RawProgress.tupled, RawProgress.unapply)
  }

  object rawProgresses extends TableQuery[RawProgresses](new RawProgresses(_))
      with MergeQuery[RawProgress, RawProgresses] {
    protected def mergeFilter(item: RawProgress) = x => x.rawDataType === item.rawDataType
  }

}
