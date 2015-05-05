package org.ukenergywatch.data

import org.scalatest._

import org.ukenergywatch.db.DbModuleMemory
import org.ukenergywatch.data._

class AggregatesTest extends FunSuite with Matchers {

  object Modules extends
      DbModuleMemory with
      DataModule

  import Modules.dataReader

  test("a") {
    dataReader.read(Seq(AllPowerStations, FuelTypeAggregate(FuelType.Solar)))
  }

}
