package org.ukenergywatch.data

import org.scalatest._

import org.ukenergywatch.db._
import org.ukenergywatch.data._
import org.ukenergywatch.utils.JavaTimeExtensions._

import scala.concurrent._
import slick.dbio._

class AggregatesTest extends FunSuite with Matchers {

  trait Components
      extends DbMemoryComponent
      with DataComponent {

    def createTables(): DBIO[_] = {
      import db.driver.api._
      val actions = db.rawDatas.schema.create >>
        db.rawProgresses.schema.create >>
        db.aggregateProgresses.schema.create >>
        db.aggregates.schema.create
      actions
    }

  }

  def m(minute: Int): DbTime = DbTime(minute.minutesToInstant)
  def d(day: Int): DbTime = DbTime(day.daysToInstant)

  test("single simple raw aggregation") {
    object Comps extends Components
    import Comps.db.driver.api._

    val draxTradingUnit = StaticData.TradingUnits.drax.name
    val drax0 = StaticData.tradingUnitsByTradingUnitName(StaticData.TradingUnits.drax).bmuIds(0).name
    val drax1 = StaticData.tradingUnitsByTradingUnitName(StaticData.TradingUnits.drax).bmuIds(1).name

    val insertRawProgress = Comps.db.rawProgresses ++= Seq(
      RawProgress(RawDataType.actualGeneration, m(0), m(60))
    )
    val insertRawData = Comps.db.rawDatas ++= Seq(
      RawData(RawDataType.actualGeneration, drax0, m(0), m(60), 1.0, 1.0),
      RawData(RawDataType.actualGeneration, drax1, m(0), m(30), 1.0, 3.0),
      RawData(RawDataType.actualGeneration, drax1, m(30), m(60), 3.0, 5.0)
    )

    val actions = Comps.createTables() >>
      insertRawProgress >> insertRawData >>
      Comps.data.createHourAggregatesFromRaw() >>
      (Comps.db.aggregates.result zip Comps.db.aggregateProgresses.result)
    val fActionResult = Comps.db.db.run(actions.withPinnedSession)
    val (aggs, prog) = Await.result(fActionResult, 1.second.toConcurrent)

    aggs.find(_.name == drax0).get.value(AggregationFunction.mean) shouldBe 1.0 +- 1e-10
    aggs.find(_.name == drax1).get.value(AggregationFunction.mean) shouldBe 3.0 +- 1e-10
    aggs.find(_.name == drax1).get.value(AggregationFunction.minimum) shouldBe 1.0 +- 1e-10
    aggs.find(_.name == drax1).get.value(AggregationFunction.maximum) shouldBe 5.0 +- 1e-10
    aggs.find(_.name == drax1).get.value(AggregationFunction.percentile(25)) shouldBe 2.0 +- 1e-10

    aggs.find(_.name == draxTradingUnit).get.value(AggregationFunction.mean) shouldBe 4.0 +- 1e-10
    aggs.find(_.name == draxTradingUnit).get.value(AggregationFunction.minimum) shouldBe 2.0 +- 1e-10
    aggs.find(_.name == draxTradingUnit).get.value(AggregationFunction.maximum) shouldBe 6.0 +- 1e-10

    aggs.find(_.name == Region.uk.name).get.value(AggregationFunction.mean) shouldBe 4.0 +- 1e-10
    aggs.find(_.name == Region.uk.name).get.value(AggregationFunction.percentile(25)) shouldBe 3.0 +- 1e-10

    prog.map(_.id0) shouldBe Seq(AggregateProgress(
      AggregationInterval.hour, AggregationType.generationUnit, m(0), m(60)
    ))
  }

  test("simple aggregation aggregation") {
    object Comps extends Components
    import Comps.db.driver.api._

    val hour = AggregationInterval.hour
    val generationUnit = AggregationType.generationUnit
    val tradingUnit = AggregationType.tradingUnit
    def aggValue(mean: Double, minimum: Double, maximum: Double): Map[AggregationFunction, Double] = Map(
      // TODO: Percentiles
      AggregationFunction.mean -> mean,
      AggregationFunction.minimum -> minimum,
      AggregationFunction.maximum -> maximum
    ) ++ (0 to 100).map { i =>
      AggregationFunction.percentile(i) -> (minimum + (maximum - minimum) * i.toDouble / 100.0)
    }

    val insertHourAggs = Comps.db.aggregates ++= (0.until(24)).flatMap { hourOffset =>
      val from = m(hourOffset * 60)
      val to = m((hourOffset + 1) * 60)
      Seq(
        Aggregate(hour, generationUnit, "a", from, to, aggValue(2.0, 1.0, 3.0)),
        Aggregate(hour, generationUnit, "b", from, to, aggValue(20.0, 10.0, 30.0)),
        Aggregate(hour, tradingUnit, "a+b", from, to, aggValue(22.0, 11.0, 33.0))
      )
    }
    val insertAggProgress = Comps.db.aggregateProgresses ++= Seq(
      AggregateProgress(hour, generationUnit, m(0), m(24 * 60))
    )

    val actions = Comps.createTables() >>
      insertHourAggs >> insertAggProgress >>
      Comps.data.createSubAggregates(AggregationInterval.hour, AggregationInterval.day) >>
      (Comps.db.aggregates.result zip Comps.db.aggregateProgresses.result)
    val fActionResult = Comps.db.db.run(actions.withPinnedSession)
    val (aggs, prog) = Await.result(fActionResult, 1.second.toConcurrent)

    val aDay: Aggregate = aggs.find(x => x.name == "a" && x.aggregationInterval == AggregationInterval.day).get
    aDay.fromTime shouldBe DbTime(0)
    aDay.toTime shouldBe DbTime(86400)
    aDay.value(AggregationFunction.mean) shouldBe 2.0 +- 1e-10
    aDay.value(AggregationFunction.minimum) shouldBe 1.0 +- 1e-10
    aDay.value(AggregationFunction.maximum) shouldBe 3.0 +- 1e-10
    aDay.value(AggregationFunction.percentile(0)) shouldBe 1.0 +- 1e-10
    aDay.value(AggregationFunction.percentile(100)) shouldBe 3.0 +- 1e-10
    aDay.value(AggregationFunction.percentile(25)) shouldBe 1.5 +- 1e-10

    val abDay: Aggregate = aggs.find(x => x.name == "a+b" && x.aggregationInterval == AggregationInterval.day).get
    abDay.fromTime shouldBe DbTime(0)
    abDay.toTime shouldBe DbTime(86400)
    abDay.value(AggregationFunction.mean) shouldBe 22.0 +- 1e-10
    abDay.value(AggregationFunction.minimum) shouldBe 11.0 +- 1e-10
    abDay.value(AggregationFunction.maximum) shouldBe 33.0 +- 1e-10
    abDay.value(AggregationFunction.percentile(0)) shouldBe 11.0 +- 1e-10
    abDay.value(AggregationFunction.percentile(100)) shouldBe 33.0 +- 1e-10
    abDay.value(AggregationFunction.percentile(25)) shouldBe 16.5 +- 1e-10
  }
/*
  // This test takes a long time ~30 seconds
  test("Multi-level aggregations (limit = 10000)") {
    object Comps extends Components
    import Comps.db.driver.api._

    val draxTradingUnit = StaticData.TradingUnits.drax.name
    val drax0 = StaticData.tradingUnitsByTradingUnitName(StaticData.TradingUnits.drax).bmuIds(0).name
    val drax1 = StaticData.tradingUnitsByTradingUnitName(StaticData.TradingUnits.drax).bmuIds(1).name

    val insertRawProgress = Comps.db.rawProgresses ++= Seq(
      RawProgress(RawDataType.actualGeneration, d(0), d(1000))
    )
    val insertRawData = Comps.db.rawDatas ++= Seq(
      RawData(RawDataType.actualGeneration, drax0, d(0), d(1000), 1.0, 1.0),
      RawData(RawDataType.actualGeneration, drax1, d(0), d(100), 0.0, 0.0),
      RawData(RawDataType.actualGeneration, drax1, d(100), d(1000), 2.0, 2.0)
    )

    val actions = Comps.createTables() >>
      insertRawProgress >> insertRawData >>
      Comps.data.createHourAggregatesFromRaw(limit = 24 * 400) >>
      Comps.data.createSubAggregatesDay(limit = 1000) >>
      Comps.data.createSubAggregatesWeek(limit = 1000) >>
      Comps.data.createSubAggregatesMonth(limit = 1000) >>
      Comps.data.createSubAggregatesYear(limit = 1000) >>
      (Comps.db.aggregates.result zip Comps.db.aggregateProgresses.result)
    val fActionResult = Comps.db.db.run(actions.withPinnedSession)
    val (aggs, prog) = Await.result(fActionResult, 60.seconds.toConcurrent)


  }
*/
}
