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

class ImportersFreqTest extends FunSuite with Matchers {

  trait InlineElexonParamsComponent extends ElexonParamsComponent {
    def elexonParams = InlineElexonParams
    object InlineElexonParams extends ElexonParams {
      def key = "elexonkey"
    }
  }
  trait AppComp extends ImportersComponent
      with DbMemoryComponent
      with DownloaderFakeComponent
      with InlineElexonParamsComponent

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
      "https://api.bmreports.com/BMRS/FREQ/v1?APIKey=elexonkey&serviceType=xml&FromDateTime=2015-12-01%2000:00:00&ToDateTime=2015-12-01%2000:05:00"
        -> ElexonResponses.freqError_BadFormat
    )

    val dbioAction = App.importers.importFreq(
      LocalDateTime.of(2015, 12, 1, 0, 0, 0), LocalDateTime.of(2015, 12, 1, 0, 5, 0))
    val f = App.db.db.run(dbioAction.transactionally)
    an [ImportException] should be thrownBy Await.result(f, 1.second.toConcurrent)
  }

  test("good import") {
    object App extends AppComp
    import App.db.driver.api._

    App.downloader.content = Map(
      "https://api.bmreports.com/BMRS/FREQ/v1?APIKey=elexonkey&serviceType=xml&FromDateTime=2015-12-01%2000:00:00&ToDateTime=2015-12-01%2000:05:00"
        -> ElexonResponses.freqOk_20151201_0000_0005
    )

    val fromDateTime = LocalDateTime.of(2015, 12, 1, 0, 0, 0)
    val toDateTime = LocalDateTime.of(2015, 12, 1, 0, 5, 0)
    val actions =
      App.db.createTables >>
      App.importers.importFreq(fromDateTime, toDateTime) >>
      (App.db.rawDatas.result zip App.db.rawProgresses.result)

    val f = App.db.db.run(actions.transactionally)
    val (rawDatas, rawProgresses) = Await.result(f, 5.second.toConcurrent)

    rawProgresses.map(_.id0) shouldBe Seq(
      RawProgress(RawDataType.Electric.frequency, DbTime(fromDateTime.toInstantUtc), DbTime(toDateTime.toInstantUtc))
    )

    def freq(secOfs: Int, from: Double, to: Double): RawData = RawData(
      RawDataType.Electric.frequency, "",
      DbTime(fromDateTime.toInstantUtc + secOfs.seconds),
      DbTime(fromDateTime.toInstantUtc + (secOfs + 15).seconds),
      from, to
    )
    rawDatas.map(_.id0.searchIndex0) shouldBe Seq(
      49.976, 49.941, 49.933, 49.933, 49.930, 49.917, 49.929, 49.920, 49.922, 49.914,
      49.903, 49.886, 49.885, 49.865, 49.900, 49.950, 49.967, 49.966, 49.971, 49.971, 49.976
    ).sliding(2).zipWithIndex.map { case (Seq(a, b), i) => freq(i * 15, a, b) }.toSeq
    rawDatas.head.searchIndex should not be -1
  }

}
