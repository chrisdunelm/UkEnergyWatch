package org.ukenergywatch.db

case class Aggregate(
  aggregationInterval: AggregationInterval,
  aggregationType: AggregationType,
  name: String,
  fromTime: DbTime,
  toTime: DbTime,
  value: Map[AggregationFunction, Double],
  searchIndex: Int = -1,
  id: Int = 0
) extends MergeableValue with SearchableValue {
  def id0: Aggregate = copy(id = 0)
  def searchIndex0: Aggregate = copy(searchIndex = -1)
  def withSearchIndex(searchIndex: Int): this.type = copy(searchIndex = searchIndex).asInstanceOf[this.type]
}

trait AggregateTable extends Mergeable with Searchable {
  val driver: slick.driver.JdbcDriver
  import driver.api._

  implicit val valueColumnType = MappedColumnType.base[Map[AggregationFunction, Double], String](
    map => map.toSeq.sortBy(_._1.value).map { case (k, v) => s"${k.value}=$v" } .mkString(","),
    value => value.split(',').map { item =>
      val Array(k, v) = item.split('=')
      (AggregationFunction(k.toByte), v.toDouble)
    }.toMap
  )

  class Aggregates(tag: Tag) extends Table[Aggregate](tag, "aggregates") with MergeableTable with SearchableTable {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def aggregationInterval = column[AggregationInterval]("aggregationInterval")
    def aggregationType = column[AggregationType]("aggregationType")
    def name = column[String]("name")
    def fromTime = column[DbTime]("fromTime")
    def toTime = column[DbTime]("toTime")
    def value = column[Map[AggregationFunction, Double]]("value")
    def searchIndex = column[Int]("searchIndex")
    def * = (aggregationInterval, aggregationType,
      name, fromTime, toTime, value, searchIndex, id) <> (Aggregate.tupled, Aggregate.unapply)

    def indexSearch = index("idx_aggregates_search", searchIndex)
    def indexMerge = index("idx_aggregates_merge", (aggregationInterval, aggregationType, name, searchIndex))
  }

  object aggregates extends TableQuery[Aggregates](new Aggregates(_)) with MergeQuery[Aggregate, Aggregates] {
    protected def mergeFilter(item: Aggregate) = { x =>
      x.aggregationInterval === item.aggregationInterval && x.aggregationType === item.aggregationType &&
      x.name === item.name && x.value === item.value
    }
  }

}
