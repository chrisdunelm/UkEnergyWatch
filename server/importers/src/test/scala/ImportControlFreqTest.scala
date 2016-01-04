package org.ukenergywatch.importers

import org.ukenergywatch.utils.{ ClockFakeComponent, ElexonParamsComponent,
  DownloaderFakeComponent, LogMemoryComponent, FlagsComponent }
import org.ukenergywatch.db.DbPersistentMemoryComponent
import org.ukenergywatch.data.DataComponent
import org.ukenergywatch.db.{ RawDataType, RawData, DbTime, RawProgress }
import org.ukenergywatch.utils.JavaTimeExtensions._
import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import org.scalatest._

class ImportControlFreqTest extends FunSuite with Matchers {

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

  test("No existing data, 2 successful imports, in time order") {
    object App extends AppTemplate
    import App.db.driver.api._

    App.downloader.content = Map(
      "https://api.bmreports.com/BMRS/FREQ/v1?APIKey=elexonkey&serviceType=xml&" +
        "FromDateTime=2015-11-30%2023:00:00&ToDateTime=2015-12-01%2000:00:00"
        -> ElexonResponses.freqOk_20151130_230000_20151201_000000,
      "https://api.bmreports.com/BMRS/FREQ/v1?APIKey=elexonkey&serviceType=xml&" +
        "FromDateTime=2015-12-01%2000:00:00&ToDateTime=2015-12-01%2000:05:00"
        -> ElexonResponses.freqOk_20151201_000000_20151201_000200
    )
    App.db.executeAndWait(App.db.createTables, 1.second)

    // First import, will be 1 hour of data
    App.clock.fakeInstant = LocalDateTime.of(2015, 12, 1, 0, 0, 0).toInstantUtc
    App.importControl.freq(15.seconds)
    // Second import, will be 2 minutes of data
    App.clock.fakeInstant += 5.minutes
    App.importControl.freq(5.seconds)

    // Check import raw data
    val qRaw = App.db.rawDatas.search(
      LocalDateTime.of(2015, 1, 1, 0, 0, 0).toInstantUtc,
      LocalDateTime.of(2017, 1, 1, 0, 0, 0).toInstantUtc
    ).filter { x => x.rawDataType === RawDataType.Electric.frequency }
    val rawData = App.db.executeAndWait(qRaw.result, 4.seconds)

    rawData.sortBy(_.fromTime.value).map(x => (x.fromTime, x.fromValue)).reverse.take(10).reverse shouldBe Seq(
      (DbTime(LocalDateTime.of(2015, 11, 30, 23, 59, 30).toInstantUtc), 49.979),
      (DbTime(LocalDateTime.of(2015, 11, 30, 23, 59, 45).toInstantUtc), 49.981),
      (DbTime(LocalDateTime.of(2015, 12, 1, 0, 0,  0).toInstantUtc), 49.976),
      (DbTime(LocalDateTime.of(2015, 12, 1, 0, 0, 15).toInstantUtc), 49.941),
      (DbTime(LocalDateTime.of(2015, 12, 1, 0, 0, 30).toInstantUtc), 49.933),
      (DbTime(LocalDateTime.of(2015, 12, 1, 0, 0, 45).toInstantUtc), 49.933),
      (DbTime(LocalDateTime.of(2015, 12, 1, 0, 1,  0).toInstantUtc), 49.930),
      (DbTime(LocalDateTime.of(2015, 12, 1, 0, 1, 15).toInstantUtc), 49.917),
      (DbTime(LocalDateTime.of(2015, 12, 1, 0, 1, 30).toInstantUtc), 49.929),
      (DbTime(LocalDateTime.of(2015, 12, 1, 0, 1, 45).toInstantUtc), 49.920)
    )

    // Check raw progress
    App.db.executeAndWait(App.db.rawProgresses.result, 1.second).map(_.id0) shouldBe Seq(
      RawProgress(RawDataType.Electric.frequency,
        DbTime(LocalDateTime.of(2015, 11, 30, 23, 0, 0).toInstantUtc),
        DbTime(LocalDateTime.of(2015, 12, 1, 0, 2, 0).toInstantUtc)
      )
    )
  }

}
