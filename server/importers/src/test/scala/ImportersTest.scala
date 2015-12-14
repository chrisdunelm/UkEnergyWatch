package org.ukenergywatch.importers

import org.scalatest._
import org.ukenergywatch.utils.DownloaderFakeComponent
import org.ukenergywatch.utils.ElexonParamsComponent
import org.ukenergywatch.db.DbMemoryComponent
import java.time.LocalDate
import org.ukenergywatch.utils.StringExtensions._
import scala.concurrent.Await
import org.ukenergywatch.utils.JavaTimeExtensions._
import org.ukenergywatch.db.{ RawData, DbTime, RawProgress, RawDataType }
import org.ukenergywatch.utils.units._

import scala.concurrent.ExecutionContext.Implicits.global

class ImporterActualGenerationsTest extends FunSuite with Matchers {

  import B1610Responses._

  test("bad key") {
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
      "https://api.bmreports.com/BMRS/B1610/v1?APIKey=elexonkey&serviceType=xml&SettlementDate=2015-12-01&Period=1"
        -> b1610Error_BadKey
    )

    val dbioAction = App.importers.importActualGeneration(LocalDate.of(2015, 12, 1), 1)
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
    val createTablesAction = App.db.createTables

    App.downloader.content = Map(
      "https://api.bmreports.com/BMRS/B1610/v1?APIKey=elexonkey&serviceType=xml&SettlementDate=2015-12-01&Period=1"
        -> b1610Ok_20151201_1
    )

    val importAction = App.importers.importActualGeneration(LocalDate.of(2015, 12, 1), 1)

    val getDataAction = App.db.rawDatas.result

    val actions = createTablesAction >> importAction >> getDataAction
    val f = App.db.db.run(actions.transactionally)
    val data: Map[String, RawData] = Await.result(f, 10.second.toConcurrent).map(x => x.name -> x).toMap

    val fromTime = DbTime(LocalDate.of(2015, 12, 1).atStartOfSettlementPeriod(1).toInstant)
    val toTime = DbTime(LocalDate.of(2015, 12, 1).atStartOfSettlementPeriod(1).toInstant + 30.minutes)

    data("T_PEMB-21").fromTime shouldBe fromTime
    data("T_PEMB-21").fromValue shouldBe Power.megaWatts(277.8).watts +- 1e-10
    data("T_PEMB-21").toTime shouldBe toTime
    data("T_PEMB-21").toValue shouldBe Power.megaWatts(277.8).watts +- 1e-10

    data("T_FERR-3").fromTime shouldBe fromTime
    data("T_FERR-3").fromValue shouldBe Power.megaWatts(467.694).watts +- 1e-10
    data("T_FERR-3").toTime shouldBe toTime
    data("T_FERR-3").toValue shouldBe Power.megaWatts(467.694).watts +- 1e-10
  }

  test("multiple file good import") {
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
    val createTablesAction = App.db.createTables

    App.downloader.content = Map(
      "https://api.bmreports.com/BMRS/B1610/v1?APIKey=elexonkey&serviceType=xml&SettlementDate=2015-12-01&Period=1"
        -> b1610Ok_20151201_1,
      "https://api.bmreports.com/BMRS/B1610/v1?APIKey=elexonkey&serviceType=xml&SettlementDate=2015-12-01&Period=2"
        -> b1610Ok_20151201_2
    )

    val importAction1 = App.importers.importActualGeneration(LocalDate.of(2015, 12, 1), 1)
    val importAction2 = App.importers.importActualGeneration(LocalDate.of(2015, 12, 1), 2)

    val actions = createTablesAction >> importAction1 >> importAction2 >>
      (App.db.rawDatas.result zip App.db.rawProgresses.result)
    val f = App.db.db.run(actions.transactionally)
    val (rawDatas, rawProgresses) = Await.result(f, 10.second.toConcurrent)
    val data: Map[String, Seq[RawData]] = rawDatas.groupBy(_.name)

    data("T_PEMB-21").sortBy(_.fromTime.value).map(x => Power.watts(x.fromValue)) shouldBe Seq(
      Power.megaWatts(277.8),
      Power.megaWatts(395.3)
    )
    data("T_FERR-3").sortBy(_.fromTime.value).map(x => Power.watts(x.fromValue)) shouldBe Seq(
      Power.megaWatts(467.694),
      Power.megaWatts(466.944)
    )
    data("T_DRAXX-1").size shouldBe 1
    data("T_DRAXX-1").head.fromTime shouldBe
      DbTime(LocalDate.of(2015, 12, 1).atStartOfSettlementPeriod(1).toInstant)
    data("T_DRAXX-1").head.toTime shouldBe
      DbTime(LocalDate.of(2015, 12, 1).atStartOfSettlementPeriod(3).toInstant)
    data("T_DRAXX-1").head.fromValue shouldBe Power.megaWatts(645.136).watts
    data("T_DRAXX-1").head.toValue shouldBe Power.megaWatts(645.136).watts

    rawProgresses.map(_.id0) shouldBe Seq(
      RawProgress(
        RawDataType.actualGeneration,
        DbTime(LocalDate.of(2015, 12, 1).atStartOfSettlementPeriod(1).toInstant),
        DbTime(LocalDate.of(2015, 12, 1).atEndOfSettlementPeriod(2).toInstant)
      )
    )
  }

}
