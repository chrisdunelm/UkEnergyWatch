package org.ukenergywatch.db

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
