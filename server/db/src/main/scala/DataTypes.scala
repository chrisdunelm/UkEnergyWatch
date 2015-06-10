package org.ukenergywatch.db

import org.joda.time.{ReadableInstant, Instant}
import org.ukenergywatch.utils.JodaExtensions._

trait DataTypes {
  val driver: slick.driver.JdbcDriver
  import driver.api._

  case class AggregationInterval(val value: Int) extends MappedTo[Int]
  object AggregationInterval {
    val hour = AggregationInterval(1)
    val day = AggregationInterval(2)
    val week = AggregationInterval(3)
    val month = AggregationInterval(4)
    val year = AggregationInterval(5)
  }

  case class AggregationFunction(val value: Int) extends MappedTo[Int]
  object AggregationFunction {
    val average = AggregationFunction(1)
    val maximum = AggregationFunction(2)
    val minimum = AggregationFunction(3)
  }

  case class LocationType(val value: Int) extends MappedTo[Int]
  object LocationType {
    val generationUnit = LocationType(1)
    val tradingUnit = LocationType(2)
    val uk = LocationType(3)
  }

  case class DbTime(val value: Int) extends MappedTo[Int] {
    def toInstant: Instant = new Instant(value.toLong * 1000L)
  }
  object DbTime {
    def apply(dt: ReadableInstant): DbTime = DbTime((dt.millis / 1000L).toInt)
  }
  
}
