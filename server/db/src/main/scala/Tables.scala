package org.ukenergywatch.db

import slick.driver.JdbcDriver

class Tables(val driver: JdbcDriver) {
  import driver.api._

  case class Interval(val value: Int) extends MappedTo[Int]
  object Interval {
    val hour = Interval(1)
    val day = Interval(2)
    val week = Interval(3)
    val month = Interval(4)
    val year = Interval(5)
  }

  case class AggregateFunction(val value: Int) extends MappedTo[Int]
  object AggregateFunction {
    val average = AggregateFunction(1)
    val peak = AggregateFunction(2)
  }

  case class Location(val value: Int) extends MappedTo[Int]
  object Location {
    val genUnit = Location(1)
    val powerStation = Location(2)
    val all = Location(3)
  }

  case class Aggregate(
    interval: Interval,
    aggregateFunction: AggregateFunction,
    location: Location,
    start: Int,
    end: Int,
    id: Int = 0
  )

  class Aggregates(tag: Tag) extends Table[Aggregate](tag, "aggregate") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def interval = column[Interval]("interval")
    def aggregateFunction = column[AggregateFunction]("aggregateFunction")
    def location = column[Location]("location")
    def start = column[Int]("start")
    def end= column[Int]("end")
    def * = (interval, aggregateFunction, location, start, end, id) <> (Aggregate.tupled, Aggregate.unapply)
  }

  val aggregates = TableQuery[Aggregates]

}
