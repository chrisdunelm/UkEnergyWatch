package org.ukenergywatch.importers

import org.ukenergywatch.db.{ DbComponent, RawData, RawDataType, DbTime, RawProgress }
import org.ukenergywatch.utils.DownloaderComponent
import org.ukenergywatch.utils.ElexonParamsComponent
import java.time.LocalDate
import scala.concurrent.ExecutionContext
import slick.dbio.{ DBIOAction }
import scala.xml.XML
import org.ukenergywatch.utils.StringExtensions._
import org.ukenergywatch.data.BmuId
import org.ukenergywatch.utils.units._
import org.ukenergywatch.utils.JavaTimeExtensions._

trait ImportersComponent {
  this: DbComponent with DownloaderComponent with ElexonParamsComponent =>

  import db.driver.api._

  lazy val importers: Importers = new Importers

  class Importers {

    def makeUrl(report: String): String = {
      s"https://api.bmreports.com/BMRS/$report/v1?APIKey=${elexonParams.key}&serviceType=xml"
    }

    // settlementPeriod 1 to 50, as defined by Elexon
    def importActualGeneration(settlementDate: LocalDate, settlementPeriod: Int)(
      implicit ec: ExecutionContext): DBIO[_] = {
      val sd = settlementDate.toString
      val url = makeUrl("B1610") + s"&SettlementDate=$sd&Period=$settlementPeriod"
      println(url)
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

  }

}
