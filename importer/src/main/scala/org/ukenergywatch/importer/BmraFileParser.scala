package org.ukenergywatch.importer

import scala.util.matching.Regex
import org.joda.time._
import org.joda.time.format.DateTimeFormatterBuilder

object BmraFileParser {

  private val bmraFormatter =
    (new DateTimeFormatterBuilder)
      .appendYear(4, 4).appendLiteral(':')
      .appendMonthOfYear(2).appendLiteral(':')
      .appendDayOfMonth(2).appendLiteral(':')
      .appendHourOfDay(2).appendLiteral(':')
      .appendMinuteOfHour(2).appendLiteral(':')
      .appendSecondOfMinute(2).appendLiteral(':')
      .appendLiteral("GMT")
      .toFormatter
  implicit def parseBmraDateTime(s: String): DateTime = DateTime.parse(s, bmraFormatter)
  implicit def parseBmraDate(s: String): DateMidnight = DateMidnight.parse(s, bmraFormatter)
  implicit def parseBmraInt(s: String): Int = augmentString(s).toInt
  implicit def parseBmraDouble(s: String): Double = augmentString(s).toDouble

  def msgRx(subject: String): Regex = ("""^([\d:]{19}:GMT): subject=""" + subject + """, message=\{(.*)}$""").r
  def msgParts(s: String): Seq[(String, String)] = {
    for (part <- s.split(",")) yield part.split("=") match {
      case Array(a, b) => (a, b)
    }
  }

  sealed trait BmraDataItem {
    def publishTime: DateTime
  }

  case class BmraFpnData(ts: DateTime, vp: Double)
  case class BmraFpn(publishTime: DateTime, bmu: String, sd: DateMidnight, sp: Int, ps: Seq[BmraFpnData]) extends BmraDataItem
  private object BmraFpn {
    // 2013:10:31:09:30:55:GMT: subject=BMRA.BM.T_DRAXX-1.FPN, message={SD=2013:10:31:00:00:00:GMT,SP=22,NP=2,TS=2013:10:31:10:30:00:GMT,VP=645.0,TS=2013:10:31:11:00:00:GMT,VP=645.0}
    // 2013:10:31:09:30:55:GMT: subject=BMRA.BM.T_DNLWW-1.FPN, message={SD=2013:10:31:00:00:00:GMT,SP=22,NP=3,TS=2013:10:31:10:30:00:GMT,VP=9.0,TS=2013:10:31:10:31:00:GMT,VP=6.0,TS=2013:10:31:11:00:00:GMT,VP=6.0}
    val rx = msgRx("""BMRA\.BM\.([^.]+)\.FPN""")
    def unapply(s: String): Option[BmraFpn] = s match {
      case rx(publishTime, bmu, msg) =>
        try {
          val ps = msgParts(msg)
          val m = (ps.takeWhile { case (k, _) => k != "NP" }).toMap
          val data = for (Seq(("TS", ts), ("VP", vp)) <- ps.drop(m.size + 1).grouped(2)) yield BmraFpnData(ts, vp)
          Some(BmraFpn(publishTime, bmu, m("SD"), m("SP"), data.toSeq))
        } catch {
          case ex: Throwable =>
            // TODO: Log error
            None
        }
      case _ => None
    }
  }

  def parse(line: String): Option[BmraDataItem] = line match {
    case BmraFpn(fpn) => Some(fpn)
    case _ => None
  }
}
