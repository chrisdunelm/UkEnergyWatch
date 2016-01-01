package org.ukenergywatch.importers

import org.scalatest._
import org.ukenergywatch.utils.{ DownloaderFakeComponent, ElexonParamsComponent,
  LogMemoryComponent, ClockFakeComponent }
import org.ukenergywatch.db.DbMemoryComponent
import java.time.LocalDateTime
import org.ukenergywatch.utils.StringExtensions._
import scala.concurrent.Await
import org.ukenergywatch.utils.JavaTimeExtensions._
import org.ukenergywatch.db.{ RawData, DbTime, RawProgress, RawDataType }
import org.ukenergywatch.utils.units._

import scala.concurrent.ExecutionContext.Implicits.global

class ImportersFuelInstTest extends FunSuite with Matchers {

  trait InlineElexonParamsComponent extends ElexonParamsComponent {
    def elexonParams = InlineElexonParams
    object InlineElexonParams extends ElexonParams {
      def key = "elexonkey"
    }
  }
  trait AppTemplate extends ElectricImportersComponent
      with DbMemoryComponent
      with DownloaderFakeComponent
      with InlineElexonParamsComponent
      with LogMemoryComponent
      with ClockFakeComponent

  test("error import") {
    object App extends AppTemplate
    import App.db.driver.api._

    App.downloader.content = Map(
      "https://api.bmreports.com/BMRS/FUELINST/v1?APIKey=elexonkey&serviceType=xml&FromDateTime=2015-12-01%2000:00:00&ToDateTime=2015-12-01%2001:00:00"
        -> ElexonResponses.fuelInstError_BadFormat
    )

    val dbioAction = App.electricImporters.importFuelInst(
      LocalDateTime.of(2015, 12, 1, 0, 0, 0), LocalDateTime.of(2015, 12, 1, 1, 0, 0))
    val f = App.db.db.run(dbioAction.transactionally)
    an [ImportException] should be thrownBy Await.result(f, 1.second.toConcurrent)
  }

  test("good import") {
    object App extends AppTemplate
    import App.db.driver.api._

    App.downloader.content = Map(
      "https://api.bmreports.com/BMRS/FUELINST/v1?APIKey=elexonkey&serviceType=xml&FromDateTime=2015-12-01%2000:00:00&ToDateTime=2015-12-01%2001:00:00"
        -> ElexonResponses.fuelInstOk_20151201_00_01
    )

    val fromDateTime = LocalDateTime.of(2015, 12, 1, 0, 0, 0)
    val toDateTime = LocalDateTime.of(2015, 12, 1, 1, 0, 0)

    val actions =
      App.db.createTables >>
      App.electricImporters.importFuelInst(fromDateTime, toDateTime) >>
      (App.db.rawDatas.result zip App.db.rawProgresses.result)
    val f = App.db.db.run(actions.transactionally)
    val (rawDatas, rawProgresses) = Await.result(f, 5.second.toConcurrent)

    rawProgresses.map(_.id0) shouldBe Seq(
      RawProgress(RawDataType.Electric.generationByFuelType,
        DbTime(fromDateTime.toInstantUtc), DbTime(toDateTime.toInstantUtc))
    )

    rawDatas.filter(_.name == "oil").map(_.id0.searchIndex0) shouldBe Seq(
      RawData(
        RawDataType.Electric.generationByFuelType, "oil",
        DbTime(fromDateTime.toInstantUtc), DbTime(toDateTime.toInstantUtc),
        0.0, 0.0
      )
    )
    def windy(minOfs: Int, value: Double): RawData = RawData(
      RawDataType.Electric.generationByFuelType, "wind",
      DbTime(fromDateTime.toInstantUtc + (minOfs - 5).minutes),
      DbTime(fromDateTime.toInstantUtc + minOfs.minutes),
      value, value
    )
    rawDatas.filter(_.name == "wind").sortBy(_.fromTime.toInstant).map(_.id0.searchIndex0) shouldBe Seq(
      windy(5, 955e6), windy(10, 953e6), windy(15, 914e6), windy(20, 874e6), windy(25, 815e6), windy(30, 795e6),
      windy(35, 815e6), windy(40, 814e6), windy(45, 820e6), windy(50, 831e6), windy(55, 830e6), windy(60, 791e6)
    )
  }

}
