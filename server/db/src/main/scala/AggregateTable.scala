package org.ukenergywatch.db

trait AggregateTable {
  this: DataTypes =>
  val driver: slick.driver.JdbcDriver
  import driver.api._

  case class Aggregate(
    aggregationInterval: AggregationInterval,
    aggregationFunction: AggregationFunction,
    locationType: LocationType,
    locationName: String,
    from: DbTime,
    to: DbTime,
    value: Double,
    id: Int = 0
  )

  class Aggregates(tag: Tag) extends Table[Aggregate](tag, "aggregate") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def aggregationInterval = column[AggregationInterval]("aggregationInterval")
    def aggregationFunction = column[AggregationFunction]("aggregationFunction")
    def locationType = column[LocationType]("locationType")
    def locationName = column[String]("locationName")
    def from = column[DbTime]("from")
    def to = column[DbTime]("to")
    def value = column[Double]("value")
    def * = (aggregationInterval, aggregationFunction, locationType,
      locationName, from, to, value, id) <> (Aggregate.tupled, Aggregate.unapply)
  }

  val aggregates = TableQuery[Aggregates]

}
