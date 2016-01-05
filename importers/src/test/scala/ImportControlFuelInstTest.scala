package org.ukenergywatch.importers

import org.ukenergywatch.utils.{ ClockFakeComponent, ElexonParamsComponent,
  DownloaderFakeComponent, LogMemoryComponent, FlagsComponent }
import org.ukenergywatch.db.DbPersistentMemoryComponent
import org.ukenergywatch.data.DataComponent
import org.ukenergywatch.utils.JavaTimeExtensions._
import org.ukenergywatch.db.{ RawDataType, RawData, DbTime, RawProgress }
import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import org.scalatest._

class ImportControlFuelInstTest extends FunSuite with Matchers {

  trait InlineElexonParamsComponent extends ElexonParamsComponent {
    object elexonParams extends ElexonParams {
      val key = "elexonkey"
    }
  }
  trait AppTemplate extends ImportControlComponent
      with ClockFakeComponent
      with DbPersistentMemoryComponent
      with DataComponent
      with ElectricImportersComponent
      with InlineElexonParamsComponent
      with DownloaderFakeComponent
      with LogMemoryComponent
      with FlagsComponent

  test("pastonly null past-only import") {
    object App extends AppTemplate
    import App.db.driver.api._

    App.db.executeAndWait(App.db.createTables, 1.second)
    App.clock.fakeInstant = LocalDateTime.of(2015, 12, 1, 0, 0, 0).toInstantUtc
    App.importControl.fuelInst(true, 5.seconds)

    App.db.executeAndWait(App.db.rawDatas.result, 1.second) should have size 0
    App.db.executeAndWait(App.db.rawProgresses.result, 1.second) should have size 0
  }

  test("No existing data, 2 successful imports, in time order") {
    object App extends AppTemplate
    import App.db.driver.api._

    App.downloader.content = Map(
      "https://api.bmreports.com/BMRS/FUELINST/v1?APIKey=elexonkey&serviceType=xml&" +
        "FromDateTime=2015-11-30%2000:00:01&ToDateTime=2015-12-01%2000:00:00"
        -> ElexonResponses.fuelInstOk_20151130_000001_20151201_000000,
      "https://api.bmreports.com/BMRS/FUELINST/v1?APIKey=elexonkey&serviceType=xml&" +
        "FromDateTime=2015-12-01%2000:00:01&ToDateTime=2015-12-01%2000:05:00"
        -> ElexonResponses.fuelInstOk_20151201_000001_20151201_000500
    )
    App.db.executeAndWait(App.db.createTables, 1.second)

    // First import, will be 24 hours
    App.clock.fakeInstant = LocalDateTime.of(2015, 12, 1, 0, 1, 0).toInstantUtc
    App.importControl.fuelInst(false, 15.seconds)
    // Second import, will be 5 minutes
    App.clock.fakeInstant += 5.minutes
    App.importControl.fuelInst(false, 5.seconds)

    // Check import raw data
    val qRaw = App.db.rawDatas.search(
      LocalDateTime.of(2015, 11, 30, 23, 55, 0).toInstantUtc,
      LocalDateTime.of(2015, 12, 1, 0, 5, 0).toInstantUtc
    ).filter { x => x.rawDataType === RawDataType.Electric.generationByFuelType }
    val rawData = App.db.executeAndWait(qRaw.result, 4.seconds)

    rawData.filter(_.name == "wind").map(_.id0.searchIndex0).sortBy(_.fromTime.value) shouldBe Seq(
      RawData(RawDataType.Electric.generationByFuelType, "wind",
        DbTime(LocalDateTime.of(2015, 11, 30, 23, 55, 0).toInstantUtc),
        DbTime(LocalDateTime.of(2015, 12, 1, 0, 0, 0).toInstantUtc),
        983e6, 983e6
      ),
      RawData(RawDataType.Electric.generationByFuelType, "wind",
        DbTime(LocalDateTime.of(2015, 12, 1, 0, 0, 0).toInstantUtc),
        DbTime(LocalDateTime.of(2015, 12, 1, 0, 5, 0).toInstantUtc),
        955e6, 955e6
      )
    )

    // Check raw progress
    App.db.executeAndWait(App.db.rawProgresses.result, 1.second).map(_.id0) shouldBe Seq(
      RawProgress(RawDataType.Electric.generationByFuelType,
        DbTime(LocalDateTime.of(2015, 11, 30, 0, 0, 0).toInstantUtc),
        DbTime(LocalDateTime.of(2015, 12, 1, 0, 5, 0).toInstantUtc)
      )
    )

  }

}
