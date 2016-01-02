package org.ukenergywatch.importers

import org.ukenergywatch.db.{ DbComponent, RawData, RawDataType, DbTime, RawProgress, SearchableValue }
import org.ukenergywatch.utils.{ ElexonParamsComponent, DownloaderComponent }
import org.ukenergywatch.utils.SimpleRangeOf
import java.time.{ LocalDate, LocalDateTime }
import scala.concurrent.ExecutionContext
import slick.dbio.{ DBIOAction }
import scala.xml.{ XML, Elem, Node }
import org.ukenergywatch.utils.StringExtensions._
import org.ukenergywatch.data.{ BmuId, StaticData }
import org.ukenergywatch.utils.units._
import org.ukenergywatch.utils.JavaTimeExtensions._

trait ElectricImportersComponent {
  this: DbComponent with DownloaderComponent with ElexonParamsComponent =>

  import db.driver.api._

  lazy val electricImporters: ElectricImporters = new ElectricImporters

  class ElectricImporters {

    // B1610 - actual generation of all generating units
    // B1620 - actual per fuel type, no interconnects
    // B1630 - actual (or predicted?) solar/wind - subset of B1620
    // FUELINST - actual per fuel type, old-style, 5-min resolution and publishing
    //     Doesn't split on/off-shore - can get this from B1620 (or B1610 if I categorise all gen-units)

    def makeElexonApiUrl(report: String): String = {
      s"https://api.bmreports.com/BMRS/$report/v1?APIKey=${elexonParams.key}&serviceType=xml"
    }

    def elexonDownload(url: String)(okFn: Elem => DBIO[_])(implicit ec: ExecutionContext): DBIO[_] = {
      val fDownload = downloader.get(url)
      val dbioDownload: DBIO[Array[Byte]] = DBIOAction.from(fDownload)
      dbioDownload.flatMap { bytes =>
        val xml: Elem = XML.loadString(bytes.toStringUtf8)
        val responseMetadata = xml \ "responseMetadata"
        val httpCode = (responseMetadata \ "httpCode").text.trim.toInt
        if (httpCode == 200) {
          okFn(xml)
        } else {
          val errorType = (responseMetadata \ "errorType").text
          val description = (responseMetadata \ "description").text
          DBIOAction.failed(new ImportException(s"Download failed: '$httpCode - $errorType: $description'"))
        }
      }
    }

    // settlementPeriod 1 to 50, as defined by Elexon
    // Half-hour resolution
    def importActualGeneration(settlementDate: LocalDate, settlementPeriod: Int)(
      implicit ec: ExecutionContext): DBIO[_] = {
      val sd = settlementDate.toString
      val url = makeElexonApiUrl("B1610") + s"&SettlementDate=$sd&Period=$settlementPeriod"
      elexonDownload(url) { xml =>
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
            rawDataType = RawDataType.Electric.actualGeneration,
            name = bmuId.name,
            fromTime = DbTime(settlementInstant),
            toTime = DbTime(settlementInstant + 30.minutes),
            fromValue = power.watts,
            toValue = power.watts
          ).autoSearchIndex
          db.rawDatas.merge(rawData)
        }
        val progressAction = db.rawProgresses.merge(RawProgress(
          rawDataType = RawDataType.Electric.actualGeneration,
          fromTime = DbTime(settlementInstant),
          toTime = DbTime(settlementInstant + 30.minutes)
        ))
        (DBIOAction.seq(rawDataDBIOs: _*) >> progressAction).transactionally
      }
    }

    // 5-minute resolution
    // 'from' parameter is sometimes inclusive, sometimes exclusive. 'to' parameter is inclusive
    // Parameters are in UTC, I think.
    // The times coming back are the end of each 5-minute period
    // New data appears to be available within seconds of the time passing.
    def importFuelInst(fromTime: LocalDateTime, toTime: LocalDateTime)(implicit ec: ExecutionContext): DBIO[_] = {
      val url = makeElexonApiUrl("FUELINST") + s"&FromDateTime=${fmtLdt(fromTime)}&ToDateTime=${fmtLdt(toTime)}"
      elexonDownload(url) { xml =>
        val items = xml \ "responseBody" \ "responseList" \ "item"
        val actions: Seq[DBIO[_]] = for (item <- items) yield {
          val toTime = (item \ "publishingPeriodCommencingTime").text.trim.toLocalDateTime.toInstantUtc
          val fromTime = toTime - 5.minutes
          val fuelActions: Seq[DBIO[_]] = for (fuelType <- StaticData.fuelTypes) yield {
            val fuelPower = Power.megaWatts((item \ fuelType).text.trim.toInt)
            val rawData = RawData(
              rawDataType = RawDataType.Electric.generationByFuelType,
              name = fuelType,
              fromTime = DbTime(fromTime),
              toTime = DbTime(toTime),
              fromValue = fuelPower.watts,
              toValue = fuelPower.watts
            ).autoSearchIndex
            db.rawDatas.merge(rawData)
          }
          val progressAction = db.rawProgresses.merge(RawProgress(
            rawDataType = RawDataType.Electric.generationByFuelType,
            fromTime = DbTime(fromTime),
            toTime = DbTime(toTime)
          ))
          (DBIOAction.seq(fuelActions: _*) >> progressAction).transactionally
        }
        DBIOAction.seq(actions: _*)
      }
    }

    private def fmtLdt(dt: LocalDateTime): String =
      f"${dt.getYear}%04d-${dt.getMonthValue}%02d-${dt.getDayOfMonth}%02d%%20" +
        f"${dt.getHour}%02d:${dt.getMinute}%02d:${dt.getSecond}%02d"

    // 15-second resolution, not sure how often data is available.
    // Values are used in pairs, with each pair forming a from/to pair.
    // Parameters appear to be inclusive.
    def importFreq(fromTime: LocalDateTime, toTime: LocalDateTime)(implicit ec: ExecutionContext): DBIO[_] = {
      val url = makeElexonApiUrl("FREQ") + s"&FromDateTime=${fmtLdt(fromTime)}&ToDateTime=${fmtLdt(toTime)}"
      elexonDownload(url) { xml =>
        val items = xml \ "responseBody" \ "responseList" \ "item"
        def ldt(item: Node): LocalDateTime = {
          val localDate = (item \ "reportSnapshotTime").text.trim.toLocalDate
          val localTime = (item \ "spotTime").text.trim.toLocalTime
          localDate + localTime
        }
        // Don't try to merge frequencies, it'll almost never happen and will be slow
       val mappedItems =  items.map { item =>
          ldt(item).toInstantUtc -> (item \ "frequency").text.trim.toDouble
        }.toSeq.sortBy(_._1)
        val rawDatas: Seq[RawData] =
          for (Seq((fromTime, fromFreq), (toTime, toFreq)) <- mappedItems.sliding(2).toSeq) yield {
            if (toTime - fromTime != 15.seconds) {
              throw new ImportException("Time between frequencies is not 15 seconds")
            }
            RawData(
              rawDataType = RawDataType.Electric.frequency,
              name = "uk", // Name the same across the whole uk
              fromTime = DbTime(fromTime),
              toTime = DbTime(toTime),
              fromValue = fromFreq,
              toValue = toFreq
            ).autoSearchIndex
          }
        val minTime = mappedItems.map(_._1).min
        val maxTime = mappedItems.map(_._1).max
        val rawDataAction = db.rawDatas ++= rawDatas
        val progressAction = db.rawProgresses.merge(RawProgress(
          rawDataType = RawDataType.Electric.frequency,
          fromTime = DbTime(minTime),
          toTime = DbTime(maxTime)
        ))
        (rawDataAction >> progressAction).transactionally
      }
    }

  }

}
