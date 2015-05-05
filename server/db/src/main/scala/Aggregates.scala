package org.ukenergywatch.db

import com.softwaremill.macwire._
import slick.driver.JdbcDriver

trait DbModule {
  // Provides
  lazy val tables = wire[Tables]

  // Dependencies
  val driver: JdbcDriver
  def db: driver.backend.Database
}

class Tables(val driver: JdbcDriver) {
  import driver.api._

  case class Aggregate(aggregateId: String, fromTime: Int, toTime: Int, value: Double, id: Int = 0)

  class Aggregates(tag: Tag) extends Table[Aggregate](tag, "aggregates") {
    def id = column[Int]("id", O.PrimaryKey)
    def aggregateId = column[String]("aggregateId")
    def fromTime = column[Int]("fromTime")
    def toTime = column[Int]("toTime")
    def value = column[Double]("value")
    def * = (aggregateId, fromTime, toTime, value, id) <> (Aggregate.tupled, Aggregate.unapply)
  }

  val aggregates = TableQuery[Aggregates]

}
