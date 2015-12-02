package org.ukenergywatch.db

import java.time.{ Instant }
import org.ukenergywatch.utils.JavaTimeExtensions._
import slick.lifted.MappedTo

case class RawDataType(val value: Byte) extends MappedTo[Byte]
object RawDataType {
  val actualGeneration = RawDataType(1)
  val PredictedGeneration = RawDataType(2)
  val GenerationByFuelType = RawDataType(3)
}

case class AggregationInterval(val value: Byte) extends MappedTo[Byte]
object AggregationInterval {
  val hour = AggregationInterval(1)
  val day = AggregationInterval(2)
  val week = AggregationInterval(3)
  val month = AggregationInterval(4)
  val year = AggregationInterval(5)
}

case class AggregationType(val value: Byte) extends MappedTo[Byte]
object AggregationType {
  // For RawDataType.actionGeneration
  val generationUnit = AggregationType(1)
  val tradingUnit = AggregationType(2)
  val uk = AggregationType(3)
  // For RawDataType.GenerationByFuelType
  val fuelType = AggregationType(4)
}

case class AggregationFunction(val value: Byte) extends MappedTo[Byte]
object AggregationFunction {
  val mean = AggregationFunction(1)
  val maximum = AggregationFunction(2)
  val minimum = AggregationFunction(3)
  // TODO: Percentiles
}

case class DbTime(val value: Int) extends MappedTo[Int] {
  def toInstant: Instant = (value.toLong * 1000L).millisToInstant
}
object DbTime {
  def apply(instant: Instant): DbTime = DbTime((instant.millis / 1000L).toInt)
}
