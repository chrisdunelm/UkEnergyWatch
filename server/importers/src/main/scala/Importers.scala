package org.ukenergywatch.importers

import org.ukenergywatch.db.{ DbComponent, RawData, RawDataType, DbTime }
import org.ukenergywatch.utils.DownloaderComponent
import org.ukenergywatch.utils.ElexonParamsComponent
import java.time.LocalDate
import scala.concurrent.ExecutionContext
import slick.dbio.{ DBIO, DBIOAction }
import scala.xml.XML
import org.ukenergywatch.utils.StringExtensions._
import org.ukenergywatch.data.BmuId
import org.ukenergywatch.utils.units._
import org.ukenergywatch.utils.JavaTimeExtensions._

trait ImportersComponent {
  this: DbComponent with DownloaderComponent with ElexonParamsComponent =>

  lazy val importers: Importers = new Importers

  class Importers {

    def makeUrl(report: String): String = {
      s"https://api.bmreports.com/BMRS/$report/v1?APIKey=${elexonParams.key}&serviceType=xml"
    }

    // settlementPeriod 1 to 50, as defined by Elexon
    def importActualGeneration(settlementDate: LocalDate, settlementPeriod: Int)(
      implicit ec: ExecutionContext): DBIO[Unit] = {
      val sd = settlementDate.toString
      val url = makeUrl("B1610") + s"&SettlementDate=$sd&Period=$settlementPeriod"
      println(url)
      val fGet = downloader.get(url)
      val dbioGet: DBIO[Array[Byte]] = DBIOAction.from(fGet)
      dbioGet.flatMap { bytes =>
        val xml = XML.loadString(bytes.toStringUtf8)
        val responseMetadata = xml \ "responseMetadata"
        val httpCode = (responseMetadata \ "httpCode").text.toInt
        if (httpCode == 200) {
          val items = xml \ "responseBody" \ "responseList" \ "item"
          val rawDataDBIOs: Seq[DBIO[_]] = for (item <- items) yield {
            val bmuId = BmuId((item \ "bMUnitID").text)
            val power = Power.megaWatts((item \ "quantity").text.toDouble)
            val settlementDate = (item \ "settlementDate").text.toLocalDate
            val settlementPeriod = (item \ "settlementPeriod").text.toInt
            val settlementInstant = settlementDate.atStartOfSettlementPeriod(settlementPeriod).toInstant
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
          DBIOAction.seq(rawDataDBIOs: _*)
        } else {
          val errorType = (responseMetadata \ "errorType").text
          val description = (responseMetadata \ "description").text
          DBIOAction.failed(new ImportException(s"Download failed: '$httpCode - $errorType: $description'"))
        }
      }
    }

  }

}
