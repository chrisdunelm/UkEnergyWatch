package org.ukenergywatch.importers

import org.ukenergywatch.utils.{ ClockFakeComponent, ElexonParamsComponent, DownloaderFakeComponent }
import org.ukenergywatch.db.DbPersistentMemoryComponent
import org.ukenergywatch.data.DataComponent
import org.ukenergywatch.db.{ RawDataType, RawData, DbTime, RawProgress, AggregateProgress }
import org.ukenergywatch.db.{ AggregationInterval, AggregationType, AggregationFunction }
import org.ukenergywatch.utils.JavaTimeExtensions._
import java.time.LocalDateTime
import org.ukenergywatch.data.{ StaticData, Region }
import scala.concurrent.ExecutionContext.Implicits.global
import org.scalatest._

class ImportControlActualGenerationTest extends FunSuite with Matchers {

  test("No existing data, 2 successful imports, in time order") {

    trait InlineElexonParamsComponent extends ElexonParamsComponent {
      object elexonParams extends ElexonParams {
        val key = "elexonkey"
      }
    }

    object App extends ImportControlComponent
        with ClockFakeComponent
        with DbPersistentMemoryComponent
        with DataComponent
        with ImportersComponent
        with InlineElexonParamsComponent
        with DownloaderFakeComponent
    import App.db.driver.api._

    App.downloader.content = Map(
      "https://api.bmreports.com/BMRS/B1610/v1?APIKey=elexonkey&serviceType=xml&SettlementDate=2015-12-01&Period=1"
        -> ElexonResponses.b1610Ok_20151201_1,
      "https://api.bmreports.com/BMRS/B1610/v1?APIKey=elexonkey&serviceType=xml&SettlementDate=2015-12-01&Period=2"
        -> ElexonResponses.b1610Ok_20151201_2
    )
    App.db.executeAndWait(App.db.createTables, 1.second)

    // Perform first import
    App.clock.fakeInstant = LocalDateTime.of(2015, 12, 1, 0, 35, 0).toInstantUtc
    App.importControl.actualGeneration()

    // Perform second import
    App.clock.fakeInstant = LocalDateTime.of(2015, 12, 1, 1, 5, 0).toInstantUtc
    App.importControl.actualGeneration()

    // Check import raw data
    val qRaw = App.db.rawDatas.search(
      LocalDateTime.of(2015, 12, 1, 0, 0, 0).toInstantUtc,
      LocalDateTime.of(2015, 12, 1, 1, 0, 0).toInstantUtc
    ).filter { x => x.rawDataType === RawDataType.Electric.actualGeneration }
    val rawData = App.db.executeAndWait(qRaw.result, 1.second)

    rawData.filter(_.name == "T_DRAXX-1").map(_.id0.searchIndex0).sortBy(_.fromTime.value) shouldBe Seq(
      RawData(RawDataType.Electric.actualGeneration, "T_DRAXX-1",
        DbTime(LocalDateTime.of(2015, 12, 1, 0, 0, 0).toInstantUtc),
        DbTime(LocalDateTime.of(2015, 12, 1, 1, 0, 0).toInstantUtc),
        645.136e6, 645.136e6
      )
    )

    rawData.filter(_.name == "T_DRAXX-2").map(_.id0.searchIndex0).sortBy(_.fromTime.value) shouldBe Seq(
      RawData(RawDataType.Electric.actualGeneration, "T_DRAXX-2",
        DbTime(LocalDateTime.of(2015, 12, 1, 0, 0, 0).toInstantUtc),
        DbTime(LocalDateTime.of(2015, 12, 1, 0, 30, 0).toInstantUtc),
        645.072e6, 645.072e6
      ),
      RawData(RawDataType.Electric.actualGeneration, "T_DRAXX-2",
        DbTime(LocalDateTime.of(2015, 12, 1, 0, 30, 0).toInstantUtc),
        DbTime(LocalDateTime.of(2015, 12, 1, 1, 0, 0).toInstantUtc),
        644.908e6, 644.908e6
      )
    )

    // Check raw progress
    App.db.executeAndWait(App.db.rawProgresses.result, 1.second).map(_.id0) shouldBe Seq(
      RawProgress(RawDataType.Electric.actualGeneration,
        DbTime(LocalDateTime.of(2015, 12, 1, 0, 0, 0).toInstantUtc),
        DbTime(LocalDateTime.of(2015, 12, 1, 1, 0, 0).toInstantUtc)
      )
    )

    // Check aggregations
    val qAgg = App.db.aggregates.search(
      LocalDateTime.of(2015, 12, 1, 0, 0, 0).toInstantUtc,
      LocalDateTime.of(2015, 12, 1, 1, 0, 0).toInstantUtc
    )
    val aggData = App.db.executeAndWait(qAgg.result, 1.second)

    aggData.filter(x =>
      x.aggregationInterval == AggregationInterval.hour &&
        x.aggregationType == AggregationType.Electric.generationUnit &&
        x.name == "T_DRAXX-2"
    ).map(_.value(AggregationFunction.minimum)) shouldBe Seq(644.908e6)
    aggData.filter(x =>
      x.aggregationInterval == AggregationInterval.hour &&
        x.aggregationType == AggregationType.Electric.generationUnit &&
        x.name == "T_DRAXX-2"
    ).map(_.value(AggregationFunction.maximum)) shouldBe Seq(645.072e6)

    aggData.filter(x =>
      x.aggregationInterval == AggregationInterval.hour &&
        x.aggregationType == AggregationType.Electric.tradingUnit &&
        x.name == StaticData.TradingUnits.drax.name
    ).map(x =>
      (x.value(AggregationFunction.minimum), x.value(AggregationFunction.maximum))
    ) shouldBe Seq((3221.904e6, 3226.02e6))

    aggData.filter(x =>
      x.aggregationInterval == AggregationInterval.hour &&
        x.aggregationType == AggregationType.Electric.region &&
        x.name == Region.uk.name
    ).map(x =>
      (x.value(AggregationFunction.minimum), x.value(AggregationFunction.maximum))
    ) shouldBe Seq((26771.692e6, 26866.424e6))

    aggData.filter(_.aggregationInterval == AggregationInterval.day) should have size 0

    // Check aggregate progress
    App.db.executeAndWait(App.db.aggregateProgresses.result, 1.second).map(_.id0).toSet shouldBe Set(
      AggregateProgress(AggregationInterval.hour, AggregationType.Electric.generationUnit,
        DbTime(LocalDateTime.of(2015, 12, 1, 0, 0, 0).toInstantUtc),
        DbTime(LocalDateTime.of(2015, 12, 1, 1, 0, 0).toInstantUtc)
      ),
      AggregateProgress(AggregationInterval.hour, AggregationType.Electric.tradingUnit,
        DbTime(LocalDateTime.of(2015, 12, 1, 0, 0, 0).toInstantUtc),
        DbTime(LocalDateTime.of(2015, 12, 1, 1, 0, 0).toInstantUtc)
      ),
      AggregateProgress(AggregationInterval.hour, AggregationType.Electric.region,
        DbTime(LocalDateTime.of(2015, 12, 1, 0, 0, 0).toInstantUtc),
        DbTime(LocalDateTime.of(2015, 12, 1, 1, 0, 0).toInstantUtc)
      )
    )
  }

}
