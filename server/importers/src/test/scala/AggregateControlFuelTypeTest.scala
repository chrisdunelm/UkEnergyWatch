package org.ukenergywatch.importers

import org.ukenergywatch.db.DbPersistentMemoryComponent
import org.ukenergywatch.data.DataComponent
import org.ukenergywatch.db.{ RawData, RawDataType, DbTime, RawProgress, AggregateProgress, AggregationType, AggregationInterval, AggregationFunction }
import org.ukenergywatch.data.{ StaticData, Region }
import org.ukenergywatch.utils.JavaTimeExtensions._
import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import org.scalatest._

class AggregateControlFuelTypeTest extends FunSuite with Matchers {

  trait AppTemplate extends AggregateControlComponent
      with DbPersistentMemoryComponent
      with DataComponent

  test("Aggregate fuel types over day") {
    object App extends AppTemplate
    import App.db.driver.api._

    App.db.executeAndWait(App.db.createTables, 1.second)
    App.db.executeAndWait(
      (App.db.rawDatas ++= Seq(
        RawData(RawDataType.Electric.generationByFuelType, "wind",
          DbTime(LocalDateTime.of(2015, 12, 1, 0, 0, 0).toInstantUtc),
          DbTime(LocalDateTime.of(2015, 12, 2, 0, 0, 0).toInstantUtc),
          0.0, 24.0).autoSearchIndex,
        RawData(RawDataType.Electric.generationByFuelType, "npshyd",
          DbTime(LocalDateTime.of(2015, 12, 1, 0, 0, 0).toInstantUtc),
          DbTime(LocalDateTime.of(2015, 12, 2, 0, 0, 0).toInstantUtc),
          24.0, 0.0).autoSearchIndex
      )) >>
      (App.db.rawProgresses += RawProgress(RawDataType.Electric.generationByFuelType,
        DbTime(LocalDateTime.of(2015, 12, 1, 0, 0, 0).toInstantUtc),
        DbTime(LocalDateTime.of(2015, 12, 2, 0, 0, 0).toInstantUtc)
      )),
      1.second
    )

    App.aggregateControl.fuelInst(24, 5.seconds)

    // Check aggregate progress
    def ap(interval: AggregationInterval, typ: AggregationType): AggregateProgress = AggregateProgress(
      interval, typ,
      DbTime(LocalDateTime.of(2015, 12, 1, 0, 0, 0).toInstantUtc),
      DbTime(LocalDateTime.of(2015, 12, 2, 0, 0, 0).toInstantUtc)
    )
    App.db.executeAndWait(App.db.aggregateProgresses.result, 2.seconds).map(_.id0).toSet shouldBe Set(
      ap(AggregationInterval.hour, AggregationType.Electric.fuelType),
      ap(AggregationInterval.hour, AggregationType.Electric.regionalFuelType),
      ap(AggregationInterval.day, AggregationType.Electric.fuelType),
      ap(AggregationInterval.day, AggregationType.Electric.regionalFuelType)
    )

    // Check aggs
    val aggs = App.db.executeAndWait(App.db.aggregates.result, 2.seconds)
    aggs.filter(x => x.aggregationInterval == AggregationInterval.hour &&
      x.aggregationType == AggregationType.Electric.fuelType && x.name == "wind")
      .sortBy(_.fromTime.value)
      .map(_.value(AggregationFunction.mean)) shouldBe (0 until 24).map(_.toDouble + 0.5)
    aggs.filter(x => x.aggregationInterval == AggregationInterval.hour &&
      x.aggregationType == AggregationType.Electric.fuelType && x.name == "npshyd")
      .sortBy(_.fromTime.value)
      .map(_.value(AggregationFunction.mean)) shouldBe (0 until 24).map(23.0 - _.toDouble + 0.5)
    aggs.filter(x => x.aggregationInterval == AggregationInterval.hour &&
      x.aggregationType == AggregationType.Electric.regionalFuelType && x.name == Region.uk.name)
      .sortBy(_.fromTime.value)
      .map(_.value(AggregationFunction.mean)) shouldBe (0 until 24).map(_ => 24.0)
    aggs.filter(x => x.aggregationInterval == AggregationInterval.day &&
      x.aggregationType == AggregationType.Electric.fuelType && x.name == "wind")
      .sortBy(_.fromTime.value)
      .map(_.value(AggregationFunction.mean)) shouldBe Seq(12.0)
    aggs.filter(x => x.aggregationInterval == AggregationInterval.day &&
      x.aggregationType == AggregationType.Electric.fuelType && x.name == "npshyd")
      .sortBy(_.fromTime.value)
      .map(_.value(AggregationFunction.mean)) shouldBe Seq(12.0)
    aggs.filter(x => x.aggregationInterval == AggregationInterval.day &&
      x.aggregationType == AggregationType.Electric.regionalFuelType && x.name == Region.uk.name)
      .sortBy(_.fromTime.value)
      .map(_.value(AggregationFunction.mean)) shouldBe Seq(24.0)
  }

}
