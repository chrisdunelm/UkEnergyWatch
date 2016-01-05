package org.ukenergywatch.db

import org.scalatest._

class DataTypesTest extends FunSuite with Matchers {

  test("RawDataType toString") {
    RawDataType.Electric.actualGeneration.toString shouldBe "Electric.actualGeneration"
  }

  test("AggregationType toString") {
    AggregationType.Electric.generationUnit.toString shouldBe "Electric.generationUnit"
  }

  test("AggregationInterval toString") {
    AggregationInterval.hour.toString shouldBe "hour"
  }

}
