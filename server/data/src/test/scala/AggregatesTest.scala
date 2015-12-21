package org.ukenergywatch.data

import org.scalatest._

import org.ukenergywatch.db._
import org.ukenergywatch.data._
import org.ukenergywatch.utils.SimpleRangeOf
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
      RawProgress(RawDataType.Electric.actualGeneration, m(0), m(60))
    )
    val insertRawData = Comps.db.rawDatas ++= Seq(
      RawData(RawDataType.Electric.actualGeneration, drax0, m(0), m(60), 1.0, 1.0).autoSearchIndex,
      RawData(RawDataType.Electric.actualGeneration, drax1, m(0), m(30), 1.0, 3.0).autoSearchIndex,
      RawData(RawDataType.Electric.actualGeneration, drax1, m(30), m(60), 3.0, 5.0).autoSearchIndex
    )

    val actions = Comps.createTables() >>
      insertRawProgress >> insertRawData >>
      Comps.data.actualGenerationHourAggregatesFromRaw() >>
      (Comps.db.aggregates.result zip Comps.db.aggregateProgresses.result)
    val fActionResult = Comps.db.db.run(actions.withPinnedSession)
    val (aggs, prog) = Await.result(fActionResult, 3.seconds.toConcurrent)

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

    prog.map(_.id0).toSet shouldBe Set(
      AggregateProgress(AggregationInterval.hour, AggregationType.Electric.generationUnit, m(0), m(60)),
      AggregateProgress(AggregationInterval.hour, AggregationType.Electric.tradingUnit, m(0), m(60)),
      AggregateProgress(AggregationInterval.hour, AggregationType.Electric.regionalGeneration, m(0), m(60))
    )
  }

  test("simple aggregation aggregation") {
    object Comps extends Components
    import Comps.db.driver.api._

    val hour = AggregationInterval.hour
    val generationUnit = AggregationType.Electric.generationUnit
    val tradingUnit = AggregationType.Electric.tradingUnit
    def aggValue(mean: Double, minimum: Double, maximum: Double): Map[AggregationFunction, Double] = Map(
      AggregationFunction.mean -> mean,
      AggregationFunction.minimum -> minimum,
      AggregationFunction.maximum -> maximum
    ) ++ (0 to 100).map { i =>
      AggregationFunction.percentile(i) -> (minimum + (maximum - minimum) * i.toDouble / 100.0)
    }

    val insertHourAggs = Comps.db.aggregates ++= ((0.until(24)).flatMap { hourOffset =>
      val from = m(hourOffset * 60)
      val to = m((hourOffset + 1) * 60)
      val range = SimpleRangeOf(from.toInstant, to.toInstant)
      Seq(
        Aggregate(hour, generationUnit, "a", from, to, aggValue(2.0, 1.0, 3.0)).autoSearchIndex,
        Aggregate(hour, generationUnit, "b", from, to, aggValue(20.0, 10.0, 30.0)).autoSearchIndex,
        Aggregate(hour, tradingUnit, "a+b", from, to, aggValue(22.0, 11.0, 33.0)).autoSearchIndex
      )
     })
    val insertAggProgress = Comps.db.aggregateProgresses ++= Seq(
      AggregateProgress(hour, generationUnit, m(0), m(24 * 60)),
      AggregateProgress(hour, tradingUnit, m(0), m(24 * 60))
    )

    val actions = Comps.createTables() >>
      insertHourAggs >> insertAggProgress >>
      Comps.data.actualGenerationSubAggregatesDay() >>
      (Comps.db.aggregates.result zip Comps.db.aggregateProgresses.result)
    val fActionResult = Comps.db.db.run(actions.withPinnedSession)
    val (aggs, prog) = Await.result(fActionResult, 3.seconds.toConcurrent)

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
