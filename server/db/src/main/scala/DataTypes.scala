package org.ukenergywatch.db

import org.joda.time.{ReadableInstant, Instant}
import slick.driver.JdbcDriver.api.MappedTo

import org.ukenergywatch.utils.JodaExtensions._

case class RawDataType(val value: Byte) extends MappedTo[Byte]
object RawDataType {
  object ActualGeneration extends RawDataType(1)
  object PredictedGeneration extends RawDataType(2)
  object GenerationByFuelType extends RawDataType(3)
}

case class AggregationInterval(val value: Byte) extends MappedTo[Byte]
object AggregationInterval {
  object Hour extends AggregationInterval(1)
  object Day extends AggregationInterval(2)
  object Week extends AggregationInterval(3)
  object Month extends AggregationInterval(4)
  object Year extends AggregationInterval(5)
}

case class AggregationFunction(val value: Byte) extends MappedTo[Byte]
object AggregationFunction {
  object Average extends AggregationFunction(1)
  object Maximum extends AggregationFunction(2)
  object Minimum extends AggregationFunction(3)
}

case class AggregationType(val value: Byte) extends MappedTo[Byte]
object AggregationType {
  object GenerationUnit extends AggregationType(1)
  object TradingUnit extends AggregationType(2)
  object Uk extends AggregationType(3)
  object FuelType extends AggregationType(4)
}

case class DbTime(val value: Int) extends MappedTo[Int] {
  def toInstant: Instant = new Instant(value.toLong * 1000L)
}
object DbTime {
  def apply(dt: ReadableInstant): DbTime = DbTime((dt.millis / 1000L).toInt)
}
