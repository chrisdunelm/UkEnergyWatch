package org.ukenergywatch.importers

import org.ukenergywatch.db.{ DbComponent, RawData, RawDataType, DbTime, RawProgress }
import org.ukenergywatch.utils.DownloaderComponent
import org.ukenergywatch.utils.ElexonParamsComponent
import java.time.{ LocalDate, LocalDateTime }
import scala.concurrent.ExecutionContext
import slick.dbio.{ DBIOAction }
import scala.xml.XML
import org.ukenergywatch.utils.StringExtensions._
import org.ukenergywatch.data.{ BmuId, StaticData }
import org.ukenergywatch.utils.units._
import org.ukenergywatch.utils.JavaTimeExtensions._

trait ImportersComponent {
  this: DbComponent with DownloaderComponent with ElexonParamsComponent =>

  import db.driver.api._

  lazy val importers: Importers = new Importers

  class Importers {

    // B1610 - actual generation of all generating units
    // B1620 - actual per fuel type, no interconnects
    // B1630 - actual (or predicted?) solar/wind - subset of B1620
    // FUELINST - actual per fuel type, old-style, 5-min resolution and publishing
    //     Doesn't split on/off-shore - can get this from B1620 (or B1610 if I categorise all gen-units)

    def makeElexonApiUrl(report: String): String = {
      s"https://api.bmreports.com/BMRS/$report/v1?APIKey=${elexonParams.key}&serviceType=xml"
    }

    // settlementPeriod 1 to 50, as defined by Elexon
    // Half-hour resolution
    def importActualGeneration(settlementDate: LocalDate, settlementPeriod: Int)(
      implicit ec: ExecutionContext): DBIO[_] = {
      val sd = settlementDate.toString
      val url = makeElexonApiUrl("B1610") + s"&SettlementDate=$sd&Period=$settlementPeriod"
      val fGet = downloader.get(url)
      val dbioGet: DBIO[Array[Byte]] = DBIOAction.from(fGet)
      dbioGet.flatMap { bytes =>
        val xml = XML.loadString(bytes.toStringUtf8)
        val responseMetadata = xml \ "responseMetadata"
        val httpCode = (responseMetadata \ "httpCode").text.trim.toInt
        if (httpCode == 200) {
          val items = xml \ "responseBody" \ "responseList" \ "item"
          val settlementDate0 = (items.head \ "settlementDate").text.trim.toLocalDate
          val settlementPeriod0 = (items.head \ "settlementPeriod").text.trim.toInt
          val settlementInstant = settlementDate0.atStartOfSettlementPeriod(settlementPeriod0).toInstant
          val rawDataDBIOs: Seq[DBIO[_]] = for (item <- items) yield {
            val bmuId = BmuId((item \ "bMUnitID").text.trim)
            val power = Power.megaWatts((item \ "quantity").text.trim.toDouble)
            val settlementDate = (item \ "settlementDate").text.trim.toLocalDate
            val settlementPeriod = (item \ "settlementPeriod").text.trim.toInt
            if (settlementDate != settlementDate0 || settlementPeriod != settlementPeriod0) {
              throw new Exception("Invalid settlement time")
            }
            val rawData = RawData(
              rawDataType = RawDataType.actualGeneration,
              name = bmuId.name,
              fromTime = DbTime(settlementInstant),
              toTime = DbTime(settlementInstant + 30.minutes),
              fromValue = power.watts,
              toValue = power.watts
            )
            db.rawDatas.merge(rawData)
          }
          val progressAction = db.rawProgresses.merge(RawProgress(
            rawDataType = RawDataType.actualGeneration,
            fromTime = DbTime(settlementInstant),
            toTime = DbTime(settlementInstant + 30.minutes)
          ))
          (DBIOAction.seq(rawDataDBIOs: _*) >> progressAction).transactionally
        } else {
          val errorType = (responseMetadata \ "errorType").text
          val description = (responseMetadata \ "description").text
          DBIOAction.failed(new ImportException(s"Download failed: '$httpCode - $errorType: $description'"))
        }
      }
    }

    // 5-minute resolution
    // Parameters are inclusive, and the times coming back are the end of each 5-minute period
    // New data appears to be available within seconds of the time passing.
    def importFuelInst(fromTime: LocalDateTime, toTime: LocalDateTime)(implicit ec: ExecutionContext): DBIO[_] = {
      def fmt(dt: LocalDateTime): String =
        f"${dt.getYear}%04d-${dt.getMonthValue}%02d-${dt.getDayOfMonth}%02d%%20" +
          f"${dt.getHour}%02d:${dt.getMinute}%02d:${dt.getSecond}%02d"
      val fromFmt = fromTime.toString
      val url = makeElexonApiUrl("FUELINST") + s"&FromDateTime=${fmt(fromTime)}&ToDateTime=${fmt(toTime)}"
      println(url)
      val fGet = downloader.get(url)
      val dbioGet: DBIO[Array[Byte]] = DBIOAction.from(fGet)
      dbioGet.flatMap { bytes =>
        val xml = XML.loadString(bytes.toStringUtf8)
        val responseMetadata = xml \ "responseMetadata"
        val httpCode = (responseMetadata \ "httpCode").text.trim.toInt
        if (httpCode == 200) {
          val items = xml \ "responseBody" \ "responseList" \ "item"
          val actions: Seq[DBIO[_]] = for (item <- items) yield {
            val toTime = (item \ "publishingPeriodCommencingTime").text.trim.toLocalDateTime.toInstantUtc
            val fromTime = toTime - 5.minutes
            val fuelActions: Seq[DBIO[_]] = for (fuelType <- StaticData.fuelTypes) yield {
              val fuelPower = Power.megaWatts((item \ fuelType).text.trim.toInt)
              val rawData = RawData(
                rawDataType = RawDataType.generationByFuelType,
                name = fuelType,
                fromTime = DbTime(fromTime),
                toTime = DbTime(toTime),
                fromValue = fuelPower.watts,
                toValue = fuelPower.watts
              )
              db.rawDatas.merge(rawData)
            }
            val progressAction = db.rawProgresses.merge(RawProgress(
              rawDataType = RawDataType.generationByFuelType,
              fromTime = DbTime(fromTime),
              toTime = DbTime(toTime)
            ))
            (DBIOAction.seq(fuelActions: _*) >> progressAction).transactionally
          }
          DBIOAction.seq(actions: _*)
        } else {
          val errorType = (responseMetadata \ "errorType").text
          val description = (responseMetadata \ "description").text
          DBIOAction.failed(new ImportException(s"Download failed: '$httpCode - $errorType: $description'"))
        }
      }
    }

  }

}
