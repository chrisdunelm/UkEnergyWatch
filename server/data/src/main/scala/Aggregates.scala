package org.ukenergywatch.data

import com.softwaremill.macwire._
import org.ukenergywatch.db.Tables

trait DataModule {
  // Provides
  def dataReader = wire[DataReader]

  // Dependencies
  def tables: Tables
}

trait Aggregate {
  def key: String
  def value: String
}

trait LocationAggregate extends Aggregate

case class GenerationUnitAggregate(bmu: String) extends LocationAggregate {
  override val key: String = "gu"
  override def value: String = bmu
}
case object AllGenerationUnits extends LocationAggregate {
  override val key: String = "gu"
  override val value: String = "<all>"
}

case class PowerStationAggregate(name: String) extends LocationAggregate {
  override val key: String = "ps"
  override def value: String = name
}
case object AllPowerStations extends LocationAggregate {
  override val key: String = "ps"
  override val value: String = "<all>"
}

sealed trait FuelType
object FuelType {
  case object Solar extends FuelType
  case object WindOnshore extends FuelType
  case object WindOffshore extends FuelType
}

case class FuelTypeAggregate(fuelType: FuelType) extends Aggregate {
  override val key: String = "ft"
  override def value: String = fuelType.toString
}


class DataReader(tables: Tables) {

  def read(aggregates: Seq[Aggregate]): Unit = {
    val aggregateId = aggregates
      .sortBy(_.key)
      .map(x => s"${x.key}=${x.value}")
      .mkString(";")
    println(aggregateId)
  }

}

class DataWriter(tables: Tables) {
}

/*sealed trait Aggregate {
  def id: String
}
object Aggregate {
  case object GenUnit extends Aggregate {
    val id: String = "gu"
  }
  case object PowerStation extends Aggregate {
    val id: String = "ps"
  }
  case object FuelType extends Aggregate {
    val id: String = "ft"
  }
  case object Duration extends Aggregate {
    val id: String = "d"
  }
}

object Aggregates {

  val preCalculated = Seq(
    //Seq(
  )

  //def getData(range: Interval, 

}
 */
