package org.ukenergywatch.db

trait AggregateTable {
  val driver: slick.driver.JdbcDriver
  import driver.api._

  case class Aggregate(
    aggregationInterval: AggregationInterval,
    aggregationType: AggregationType,
    locationName: String,
    from: DbTime,
    to: DbTime,
    value: Map[AggregationFunction, Double],
    id: Long = 0
  )

  implicit val valueColumnType = MappedColumnType.base[Map[AggregationFunction, Double], String](
    map => map.map { case (k, v) => s"${k.value}=$v" } .mkString(","),
    value => value.split(',').map { item =>
      val Array(k, v) = item.split('=')
      (AggregationFunction(k.toByte), v.toDouble)
    }.toMap
  )

  class Aggregates(tag: Tag) extends Table[Aggregate](tag, "aggregate") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def aggregationInterval = column[AggregationInterval]("aggregationInterval")
    def aggregationType = column[AggregationType]("aggregationType")
    def locationName = column[String]("locationName")
    def from = column[DbTime]("from")
    def to = column[DbTime]("to")
    def value = column[Map[AggregationFunction, Double]]("value")
    def * = (aggregationInterval, aggregationType,
      locationName, from, to, value, id) <> (Aggregate.tupled, Aggregate.unapply)
  }

  val aggregates = TableQuery[Aggregates]

}
