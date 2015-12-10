package org.ukenergywatch.db

import java.time.{ Instant }
import org.ukenergywatch.utils.JavaTimeExtensions._
import slick.lifted.MappedTo

case class RawDataType(val value: Byte) extends MappedTo[Byte]
object RawDataType {
  val actualGeneration = RawDataType(1)
  val predictedGeneration = RawDataType(2)
  val generationByFuelType = RawDataType(3)
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
  val region = AggregationType(3) // E.g. country
  // For RawDataType.GenerationByFuelType
  val fuelType = AggregationType(4)
}

case class AggregationFunction(val value: Byte) extends MappedTo[Byte]
object AggregationFunction {
  def percentile(percent: Int) = {
    assert(percent >= 0 && percent <= 100)
    AggregationFunction(percent.toByte)
  }
  val mean = AggregationFunction(101)
  val maximum = AggregationFunction(102)
  val minimum = AggregationFunction(103)
}

case class DbTime(val value: Int) extends MappedTo[Int] {
  def toInstant: Instant = (value.toLong * 1000L).millisToInstant
}
object DbTime {
  def apply(instant: Instant): DbTime = DbTime((instant.millis / 1000L).toInt)
}
