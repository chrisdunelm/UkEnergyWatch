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

// TODO: Split and rename these files

case class AggregateProgress(
  aggregationInterval: AggregationInterval,
  aggregationType: AggregationType,
  fromTime: DbTime,
  toTime: DbTime,
  id: Int = 0
) extends MergeableValue {
  def id0: AggregateProgress = copy(id = 0)
}

trait AggregateProgressTable extends Mergeable {
  val driver: slick.driver.JdbcDriver
  import driver.api._

  class AggregateProgresses(tag: Tag) extends Table[AggregateProgress](tag, "aggregateprogress")
      with MergeableTable {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def aggregationInterval = column[AggregationInterval]("aggregationInterval")
    def aggregationType = column[AggregationType]("aggreationType")
    def fromTime = column[DbTime]("fromTime")
    def toTime = column[DbTime]("toTime")
    def * = (aggregationInterval, aggregationType, fromTime, toTime, id) <>
      (AggregateProgress.tupled, AggregateProgress.unapply)
  }

  object aggregateProgresses extends TableQuery[AggregateProgresses](new AggregateProgresses(_))
      with MergeQuery[AggregateProgress, AggregateProgresses] {
    protected def mergeFilter(item: AggregateProgress) = { x =>
      x.aggregationInterval === item.aggregationInterval && x.aggregationType === item.aggregationType
    }
  }

}
