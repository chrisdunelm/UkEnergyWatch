package org.ukenergywatch.importers

import org.scalatest._
import org.ukenergywatch.utils.DownloaderFakeComponent
import org.ukenergywatch.utils.ElexonParamsComponent
import org.ukenergywatch.db.DbMemoryComponent
import java.time.LocalDateTime
import org.ukenergywatch.utils.StringExtensions._
import scala.concurrent.Await
import org.ukenergywatch.utils.JavaTimeExtensions._
import org.ukenergywatch.db.{ RawData, DbTime, RawProgress, RawDataType }
import org.ukenergywatch.utils.units._

import scala.concurrent.ExecutionContext.Implicits.global

class ImportersFuelInstTest extends FunSuite with Matchers {

  test("error import") {
    trait InlineElexonParamsComponent extends ElexonParamsComponent {
      def elexonParams = InlineElexonParams
      object InlineElexonParams extends ElexonParams {
        def key = "elexonkey"
      }
    }
    object App extends ImportersComponent
        with DbMemoryComponent
        with DownloaderFakeComponent
        with InlineElexonParamsComponent

    import App.db.driver.api._

    App.downloader.content = Map(
      "https://api.bmreports.com/BMRS/FUELINST/v1?APIKey=elexonkey&serviceType=xml&FromDateTime=2015-12-01%2000:00:00&ToDateTime=2015-12-01%2001:00:00"
        -> ElexonResponses.fuelInstError_BadFormat
    )

    val dbioAction = App.importers.importFuelInst(
      LocalDateTime.of(2015, 12, 1, 0, 0, 0), LocalDateTime.of(2015, 12, 1, 1, 0, 0))
    val f = App.db.db.run(dbioAction.transactionally)
    an [ImportException] should be thrownBy Await.result(f, 1.second.toConcurrent)
  }

  test("good import") {
    trait InlineElexonParamsComponent extends ElexonParamsComponent {
      def elexonParams = InlineElexonParams
      object InlineElexonParams extends ElexonParams {
        def key = "elexonkey"
      }
    }
    object App extends ImportersComponent
        with DbMemoryComponent
        with DownloaderFakeComponent
        with InlineElexonParamsComponent

    import App.db.driver.api._

    App.downloader.content = Map(
      "https://api.bmreports.com/BMRS/FUELINST/v1?APIKey=elexonkey&serviceType=xml&FromDateTime=2015-12-01%2000:00:00&ToDateTime=2015-12-01%2001:00:00"
        -> ElexonResponses.fuelInstOk_20151201_00_01
    )

    val actions =
      App.db.createTables >>
      App.importers.importFuelInst(
        LocalDateTime.of(2015, 12, 1, 0, 0, 0), LocalDateTime.of(2015, 12, 1, 1, 0, 0)) >>
      (App.db.rawDatas.result zip App.db.rawProgresses.result)
    val f = App.db.db.run(actions.transactionally)
    val (rawDatas, rawProgresses) = Await.result(f, 1.second.toConcurrent)
    // TODO: Finish test
  }

}
