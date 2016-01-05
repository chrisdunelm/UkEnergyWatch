package org.ukenergywatch.db

import java.time.{ Instant }
import org.ukenergywatch.utils.JavaTimeExtensions._
import slick.lifted.MappedTo

trait Named[A <: MappedTo[Byte]] {
  import scala.reflect.runtime._
  import scala.reflect.runtime.universe._
  private def get(parent: Any, name: Seq[String]): Iterable[(Byte, Seq[String])] = {
    val im = currentMirror.reflect(parent)
    val members = im.symbol.asClass.typeSignature.members
    members.filter(_.isTerm).map(_.asTerm).collect {
      case s if s.isModule =>
        val child = currentMirror.reflectModule(s.asModule)
        get(child.instance, name :+ s.name.toString)
      case s if s.isAccessor =>
        val method = im.reflectMethod(s.asMethod)
        val value = method().asInstanceOf[A].value
        Seq(value -> (name :+ s.name.toString))
    }.flatten
  }
  def names: Map[Byte, String] = get(this, Seq.empty).toMap.mapValues(_.mkString("."))
}

case class RawDataType(val value: Byte) extends MappedTo[Byte] {
  override def toString: String = RawDataType.names(value)
}
object RawDataType extends Named[RawDataType] {
  object Electric {
    val actualGeneration = RawDataType(1)
    val predictedGeneration = RawDataType(2)
    val generationByFuelType = RawDataType(3)
    val frequency = RawDataType(4)
  }

  object Gas {
  }
}

case class AggregationType(val value: Byte) extends MappedTo[Byte] {
  override def toString: String = AggregationType.names(value)
}
object AggregationType extends Named[AggregationType] {
  object Electric {
    val generationUnit = AggregationType(1)
    val tradingUnit = AggregationType(2)
    val regionalGeneration = AggregationType(3)
    val fuelType = AggregationType(4) // TODO: rename to generationByFuelType
    val regionalFuelType = AggregationType(5)
    val frequency = AggregationType(6)
  }

  object Gas {
  }
}

case class AggregationInterval(val value: Byte) extends MappedTo[Byte] {
  override def toString: String = AggregationInterval.names(value)
}
object AggregationInterval extends Named[AggregationInterval] {
  val hour = AggregationInterval(1)
  val day = AggregationInterval(2)
  val week = AggregationInterval(3)
  val month = AggregationInterval(4)
  val year = AggregationInterval(5)
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
