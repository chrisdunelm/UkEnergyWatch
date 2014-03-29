package org.ukenergywatch.importer

import org.ukenergywatch.db.DalComp
import org.ukenergywatch.utils.ClockComp
import org.ukenergywatch.utils.Options
import org.ukenergywatch.utils.OptionSpec
import org.ukenergywatch.utils.RealClockComp
import org.ukenergywatch.utils.Slogger
import org.joda.time._
import org.ukenergywatch.utils.JodaTimeExtensions._

object Importer {

  sealed trait Mode
  case object CreateTables extends Mode
  case object OldBmUnits extends Mode
  case object CurrentBmUnits extends Mode

  object Flags extends Options {
    register(org.ukenergywatch.slogger.impl.Slogger.Flags)

    val mode = opt[Mode](OptionSpec(help = "The main mode in which this Importer runs"))
    val elexonKey = opt[String]("")

    val mysqlHost = opt[String]
    val mysqlDatabase = opt[String]
    val mysqlUser = opt[String]
    val mysqlPassword = opt[String]
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
      with RealHttpFetcherComp
      with FlagsConfigComp
      with MysqlDalComp
      with RealClockComp

    Runner.run(Flags.mode())
  }

}

trait RealImporter extends Slogger {
  this: BmraFileDownloaderComp with DalComp with ClockComp =>
  import BmraFileParser._
  import Importer._
  import dal._
  import dal.profile.simple._

  def run(mode: Mode): Unit = {
    log.info(s"Importer running - mode = '$mode'")
    mode match {
      case CreateTables => createTables()
      case OldBmUnits => importOldBmUnits()
      case CurrentBmUnits => importCurrentBmUnits()
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

  def importOldBmUnits() {
    // Find most recent gap
    database withSession { implicit session =>
      //println(Downloads.list)
      for {
        gap <- Downloads.getLastGap(Downloads.TYPE_BMUFPN)
        if gap.getEnd >= new DateTime(2003, 1, 1, 0, 0)
      } {
        val dayStart = if (gap.getEnd.getMillisOfDay == 0) {
          gap.getEnd - 24.hours
        } else {
          gap.getEnd.toDateTime.withMillisOfDay(0)
        }
        //println(s"dayStart = '$dayStart'")
        val lines0 = try {
          Some(bmraFileDownloader.getDay(dayStart))
        } catch {
          case e: Throwable =>
            log.warn(s"Failed to download day file at: '${dayStart}'", e)
            None
        }
        lines0 match {
          case Some(lines) =>
            for (line <- lines) {
              BmraFileParser.parse(line) match {
                case Some(dataItem) if gap.contains(dataItem.publishTime) => dataItem match {
                  case fpn: BmraFpn => insertFpnData(fpn)
                  case _ => // Do nothing
                }
                case _ => // Do nothing
              }
            }
            val download = Download(0, Downloads.TYPE_BMUFPN, dayStart.totalSeconds, gap.getEnd.totalSeconds)
            Downloads.mergeInsert(download)
          case _ => // Do nothing
        }
      }
    }
  }

  def importCurrentBmUnits() { //dal: org.ukenergywatch.db.DAL, database: scala.slick.jdbc.JdbcBackend.Database) {
    import dal._
    import dal.profile.simple._
    // Attempt to download latest half-hour file, if current time is greater than file endtime
    database withSession { implicit session =>
      val latest = Downloads.getLatest(Downloads.TYPE_BMUFPN)
      val nextFileTime = latest match {
        case Some(dt) if dt > clock.nowUtc() - 48.hours =>
          // Previous data exists, use end time
          // But not it it's too soon for the next download to be available
          if (dt + 31.minutes < clock.nowUtc()) {
            Some(dt)
          } else {
            None
          }
        case _ =>
          // No data exists, start afresh
          val useTime = clock.nowUtc() - 45.minutes
          Some(useTime.withMillisOfSecond(0).withSecondOfMinute(0).withMinuteOfHour(if (useTime.getMinuteOfHour < 30) 0 else 30))
      }
      log.info(s"nextFileTime = $nextFileTime")
      val lines0 = try {
        nextFileTime.map(bmraFileDownloader.getHalfHour(_))
      } catch {
        // TODO: Catch only correct exception(s)
        case e: Throwable =>
          log.warn(s"Failed to download HH file at: '${nextFileTime}'", e)
          None
      }
      lines0 match {
        case Some(lines) =>
          log.info(s"Downloaded file from elexon")
          for (line <- lines) {
            BmraFileParser.parse(line) match {
              case Some(dataItem) => dataItem match {
                case fpn: BmraFpn => insertFpnData(fpn)
                case _ => // Do nothing
              }
              case None => // Do nothing
            }
          }
          val t0 = (nextFileTime.get.getMillis / 1000L).toInt
          val t1 = ((nextFileTime.get + 30.minutes).getMillis / 1000L).toInt
          val download = Download(0, Downloads.TYPE_BMUFPN, t0, t1)
          Downloads.mergeInsert(download)
        case _ =>
          // Do nothing
      }
    }
  }

  def insertFpnData(fpn: BmraFpn)(implicit session: Session) {
    for {
      Seq(a, b) <- fpn.ps.sliding(2)
      if a.ts != b.ts
    } {
      val t0 = (a.ts.getMillis / 1000L).toInt
      val t1 = (b.ts.getMillis / 1000L).toInt
      val ins = BmUnitFpn(0, fpn.bmu, t0, a.vp.toFloat, t1, b.vp.toFloat)
      BmUnitFpns.mergeInsert(ins)
    }
  }

}
