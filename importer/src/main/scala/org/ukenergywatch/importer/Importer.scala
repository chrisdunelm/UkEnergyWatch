package org.ukenergywatch.importer

import org.ukenergywatch.db.DalComp
import org.ukenergywatch.utils.ClockComp
import org.ukenergywatch.utils.Options
import org.ukenergywatch.utils.OptionSpec
import org.ukenergywatch.utils.RealClockComp
import org.ukenergywatch.utils.Slogger
import org.joda.time._
import org.joda.time.format.DateTimeFormatterBuilder
import org.ukenergywatch.utils.JodaTimeExtensions._

import generated.InstantaneousFlowWebServiceSoap

object Importer {

  sealed trait Mode
  case object CreateTables extends Mode
  case object DropTables extends Mode
  case object ImportElecOld extends Mode
  case object ImportElecCurrent extends Mode
  case object ImportElecLiveGenByFuel extends Mode
  case object ImportElecLiveGridFrequency extends Mode
  case object ImportGas extends Mode

  object Flags extends Options {
    register(org.ukenergywatch.slogger.impl.Slogger.Flags)

    val mode = opt[Mode](OptionSpec(help = "The main mode in which this Importer runs"))
    val elexonKey = opt[String]("")

    val mysqlHost = opt[String]("localhost")
    val mysqlDatabase = opt[String]("")
    val mysqlUser = opt[String]("")
    val mysqlPassword = opt[String]("")

    val dropPassword = opt("", OptionSpec(help = "To drop tables put today's date here: yyyy-mm-dd"))
  }

  trait FlagsConfigComp extends ConfigComp {
    def config = FlagsConfig
    object FlagsConfig extends Config {
      def getString(key: String): Option[String] = Some(key match {
        case "elexonKey" => Flags.elexonKey()
      })
    }
  }

  trait MysqlDalComp extends DalComp {
    val dal = MysqlDal
    object MysqlDal extends Dal {
      val profile = scala.slick.driver.MySQLDriver
      import Flags._
      val database = profile.simple.Database.forURL(s"jdbc:mysql://${mysqlHost()}/${mysqlDatabase()}", mysqlUser(), mysqlPassword(), driver = "com.mysql.jdbc.Driver")
    }
  }

  def main(args: Array[String]) {
    Flags.parse(args)

    object Runner extends RealImporter
      with HttpBmraFileDownloaderComp
      with HttpBmReportsDownloaderComp
      with RealHttpFetcherComp
      with WsdlGasDataDownloaderComp
      with FlagsConfigComp
      with MysqlDalComp
      with RealClockComp

    Runner.run(Flags.mode())
  }

}

trait RealImporter extends Slogger {
  this: BmraFileDownloaderComp with BmReportsDownloaderComp with GasDataDownloaderComp with DalComp with ClockComp =>
  import BmraFileParser._
  import Importer._
  import dal._
  import dal.profile.simple._

  def run(mode: Mode): Unit = {
    log.info(s"Importer running - mode = '$mode'")
    mode match {
      case CreateTables => createTables()
      case DropTables => dropTables()
      case ImportElecOld => importElecOld()
      case ImportElecCurrent => importElecCurrent()
      case ImportElecLiveGenByFuel => importElecLiveGenByFuel()
      case ImportElecLiveGridFrequency => importElecLiveGridFrequency()
      case ImportGas => importGas()
    }
  }

  def createTables() {
    database withSession { implicit session =>
      for (ddl <- dal.ddls) {
        try {
          ddl.create
        } catch {
          case e: java.sql.SQLException => log.error("Can't create table", e)
        }
      }
    }
  }

  def dropTables() {
    if (DateTime.now().toString("YYYY-MM-dd") != Flags.dropPassword()) {
      println("Incorrect drop password")
    } else {
      database withSession { implicit session =>
        for (ddl <- dal.ddls) {
          try {
            ddl.drop
          } catch {
            case e: java.sql.SQLException => log.error("Can't drop table", e)
          }
        }
      }
    }
  }

  def importGas(): Unit = {
    database withSession { implicit session =>
      val availDt = gasDataDownloader.getLatestPublicationTime()
      log.info(s"Gas latest publication time: '$availDt'")
      val lastPublishedTime = Downloads.getLatest(Downloads.TYPE_GAS_PUBLISHED) match {
        case Some(gotDt) if availDt > gotDt => Some(gotDt)
        case Some(gotDt) => None
        case None => Some(availDt - 12.minutes)
      }
      log.info(s"Gas last published time: '$lastPublishedTime'")
      for (lastPublishedTime <- lastPublishedTime) {
        // Download data it new data available
        val data = gasDataDownloader.getInstantaneousFlowData()
        // Process and store gas import data
        import generated._
        var lastTimeSeconds = 0
        var firstTimeSeconds = Int.MaxValue
        for {
          supplyType: EDPEnergyGraphTableBE <- data.GetInstantaneousFlowDataResult.get.EDPReportPage.get.EDPEnergyGraphTableCollection.get.EDPEnergyGraphTableBE.map(_.get)
          obj: EDPObjectBE <- supplyType.EDPObjectCollection.get.EDPObjectBE.map(_.get)
          item: EDPEnergyDataBE <- obj.EnergyDataList.get.EDPEnergyDataBE.map(_.get)
        } {
          val supplyTypeDescription: String = supplyType.Description.get
          val location: String = obj.EDPObjectName.get
          val flowRate: Double = item.FlowRate
          val toTime: DateTime = new DateTime(item.ApplicableAt.toGregorianCalendar.getTimeInMillis, DateTimeZone.UTC)
          val toTimeSeconds = toTime.totalSeconds
          val fromTime = toTime - 2.minutes
          val fromTimeSeconds = fromTime.totalSeconds
          // Insert into database
          val gasImport = GasImport(supplyTypeDescription, location, fromTimeSeconds, toTimeSeconds, flowRate.toFloat)
          GasImports.mergeInsert(gasImport)
          // Keep track of times of data downloaded
          lastTimeSeconds = math.max(lastTimeSeconds, toTimeSeconds)
          firstTimeSeconds = math.min(firstTimeSeconds, fromTimeSeconds)
        }
        // Keep track of download
        log.info("Gas import processing complete")
        val downloadPublished = Download(Downloads.TYPE_GAS_PUBLISHED, lastPublishedTime.totalSeconds, availDt.totalSeconds)
        Downloads.mergeInsert(downloadPublished)
        val downloadData = Download(Downloads.TYPE_GAS_DATA, firstTimeSeconds, lastTimeSeconds)
        Downloads.mergeInsert(downloadData)
        log.info("Merged download")
      }
    }
  }

  case class LinesInInterval(lines: Iterator[String], interval: ReadableInterval)

  private val bmReportsDtFormatter =
    (new DateTimeFormatterBuilder)
      .appendYear(4, 4).appendLiteral('-')
      .appendMonthOfYear(2).appendLiteral('-')
      .appendDayOfMonth(2).appendLiteral(' ')
      .appendHourOfDay(2).appendLiteral(':')
      .appendMinuteOfHour(2).appendLiteral(':')
      .appendSecondOfMinute(2)
      .toFormatter
      .withZone(DateTimeZone.UTC)

  def importElecLiveGenByFuel() {
    database withSession { implicit session =>
      // Delete data older than 24 hours
      val expiry = (clock.nowUtc() - 24.hours).totalSeconds
      GenByFuelsLive.filter(_.toTime < expiry).delete
      // Import new data
      val downloadFrom = GenByFuelsLive.getLatestTime() match {
        // Download if existing data is more than 5.5 minutes old
        case Some(dt) if dt < (clock.nowUtc() - 5.5.minutes) => Some(dt)
        case None => Some(new DateTime(2000, 1, 1, 0, 0))
        case _ => None
      }
      var importCount = 0
      for (downloadFrom <- downloadFrom) {
        val xml = bmReportsDownloader.getGenByFuelType()
        for {
          inst <- xml \ "INST"
          at = DateTime.parse((inst \ "@AT").text, bmReportsDtFormatter)
          if at > downloadFrom
          fuel <- inst \ "FUEL"
        } {
          val t0 = (at - 5.minutes).totalSeconds
          val t1 = at.totalSeconds
          GenByFuelsLive += GenByFuel((fuel \ "@TYPE").text, t0, t1, (fuel \ "@VAL").text.toFloat)
          importCount += 1
        }
      }
      log.info(s"Imported $importCount entries")
    }    
  }

  def importElecLiveGridFrequency() {
    database withSession { implicit session =>
      // Delete data older than 24 hours
      var expiry = (clock.nowUtc() - 24.hours).totalSeconds
      GridFrequenciesLive.filter(_.endTime < expiry).delete
      // Import new data
      val downloadFrom = GridFrequenciesLive.getLatestTime() match {
        // Download if existing data is more than 2 minutes old
        case Some(dt) if dt < (clock.nowUtc() - 2.minutes) => Some(dt)
        case None => Some(new DateTime(2000, 1, 1, 0, 0))
        case _ => None
      }
      var importCount = 0
      for (downloadFrom <- downloadFrom) {
        val xml = bmReportsDownloader.getGridFrequency()
        for {
          item <- xml \ "ST"
          st = DateTime.parse((item \ "@ST").text, bmReportsDtFormatter)
          if st > downloadFrom
        } {
          GridFrequenciesLive += GridFrequency(st.totalSeconds, (item \ "@VAL").text.toFloat)
          importCount += 1
        }
      }
      log.info(s"Imported $importCount entries")
    }
  }

  def importElecOld() {
    // Find most recent gap
    database withSession { implicit session =>
      for {
        gap <- Downloads.getLastGap(Downloads.TYPE_BMRA)
        if gap.getEnd >= new DateTime(2003, 1, 1, 0, 0)
      } {
        val dayStart = if (gap.getEnd.getMillisOfDay == 0) {
          gap.getEnd - 24.hours
        } else {
          gap.getEnd.toDateTime.withMillisOfDay(0)
        }
        log.info(s"importOldBmUnits(): gap = $gap  dayStart = $dayStart")
        val linesInInterval = try {
          val interval = new Interval(dayStart, gap.getEnd)
          Some(LinesInInterval(bmraFileDownloader.getDay(dayStart), interval))
        } catch {
          case e: Throwable =>
            log.warn(s"Failed to download BMRA day file at: '${dayStart}'", e)
            None
        }
        processBmraLines(linesInInterval)
      }
    }
  }

  def importElecCurrent() {
    // Attempt to download latest half-hour file, if current time is greater than file endtime
    database withSession { implicit session =>
      val latest = Downloads.getLatest(Downloads.TYPE_BMRA)
      val nextFileTime = latest match {
        case Some(dt) if dt > clock.nowUtc() - 48.hours =>
          // Previous data exists, use end time
          // But not it it's too soon for the next download to be available
          if (dt + 33.minutes < clock.nowUtc()) {
            Some(dt)
          } else {
            None
          }
        case _ =>
          // No data exists, start afresh
          val useTime = clock.nowUtc() - 45.minutes
          Some(useTime.withMillisOfSecond(0).withSecondOfMinute(0).withMinuteOfHour(if (useTime.getMinuteOfHour < 30) 0 else 30))
      }
      log.info(s"importCurrentBmUnits(): nextFileTime = $nextFileTime")
      val linesInInterval = try {
        nextFileTime.map { nextFileTime =>
          val lines = bmraFileDownloader.getHalfHour(nextFileTime)
          val interval = new Interval(nextFileTime, nextFileTime + 30.minutes)
          LinesInInterval(lines, interval)
        }
      } catch {
        case e: Throwable =>
          log.warn(s"Failed to download BMRA HH file at: '${nextFileTime}'", e)
          None
      }
      processBmraLines(linesInInterval)
    }
  }

  def processBmraLines(linesInInterval: Option[LinesInInterval])(implicit session: Session): Unit = linesInInterval match {
    case Some(LinesInInterval(lines, interval)) => 
      log.info("Processing BMRA file from Elexon")
      var lineCount = 0
      for (line <- lines) {
        lineCount += 1
        BmraFileParser.parse(line) match {
          case Some(dataItem) if interval.contains(dataItem.publishTime) => dataItem match {
            case item: BmraFpn => insertFpnData(item)
            case item: BmraGridFrequency => insertGridFrequency(item)
            case item: BmraGenByFuel => insertGenByFuel(item)
          }
          case _ => // Do nothing
        }
      }
      log.info(s"Processing BMRA file complete. Line-count = $lineCount")
      val download = Download(Downloads.TYPE_BMRA, interval.getStart.totalSeconds, interval.getEnd.totalSeconds)
      Downloads.mergeInsert(download)
      log.info("Merged download")
    case None => // Do nothing
  }

  def insertFpnData(fpn: BmraFpn)(implicit session: Session): Unit = {
    for {
      Seq(a, b) <- fpn.ps.sliding(2)
      if a.ts != b.ts
    } {
      val ins = BmUnitFpn(fpn.bmu, a.ts.totalSeconds, a.vp.toFloat, b.ts.totalSeconds, b.vp.toFloat)
      BmUnitFpns.mergeInsert(ins)
    }
  }

  def insertGridFrequency(freq: BmraGridFrequency)(implicit session: Session): Unit = {
    val ins = GridFrequency(freq.ts.totalSeconds, freq.sf.toFloat)
    GridFrequencies.insert(ins)
  }

  def insertGenByFuel(gbf: BmraGenByFuel)(implicit session: Session): Unit = {
    val ins = GenByFuel(gbf.ft, gbf.ts.totalSeconds, (gbf.ts + 5.minutes).totalSeconds, gbf.fg.toFloat)
    GenByFuels.mergeInsert(ins)
  }

}
