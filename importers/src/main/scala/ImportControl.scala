package org.ukenergywatch.importers

import org.ukenergywatch.utils.{ ClockComponent, LogComponent }
import org.ukenergywatch.data.DataComponent
import org.ukenergywatch.db.DbComponent
import java.time.{ LocalDate, Instant, Duration }
import org.ukenergywatch.utils.JavaTimeExtensions._
import scala.concurrent.ExecutionContext
import org.ukenergywatch.utils.{ SimpleRangeOf, RangeOf }
import org.ukenergywatch.db.{ RawDataType, AggregationType }
import org.ukenergywatch.data.{ FuelType, Region }
import slick.dbio.DBIOAction

trait ImportControlComponent {
  this: DbComponent with DataComponent with ElectricImportersComponent with ClockComponent with LogComponent =>

  lazy val importControl = new ImportControl

  class ImportControl {
    import db.driver.api._

    // All of these as tested on 2016-01-06
    val minActualGeneration = LocalDate.of(2015, 1, 1).toInstant
    val minFuelInst = LocalDate.of(2015, 8, 1).toInstant
    val minFreq = LocalDate.of(2015, 8, 1).toInstant

    // Import some data
    // Call every five minutes, with a ~1 minute offset
    // Always gets the latest data if possible
    // Otherwise gets past data, back to some fixed point
    // This method will block for DB and network things
    def actualGeneration(pastOnly: Boolean, timeout: Duration)(implicit ec: ExecutionContext): Unit = {
      val now: Instant = clock.nowUtc()

      val extremes = SimpleRangeOf(minActualGeneration, now.alignTo(30.minutes))
      val qMissing: DBIO[Seq[RangeOf[Instant]]] =
        data.missingRawProgress(RawDataType.Electric.actualGeneration, extremes)
      val qImport = qMissing.flatMap { missing: Seq[RangeOf[Instant]] =>
        val useRange = if (pastOnly) {
          missing.filter(_.to < extremes.to).lastOption
        } else {
          missing.lastOption
        }
        useRange match {
          case Some(range) =>
            // Import most recent data that isn't already imported
            assert((range.to - range.from) >= 30.minutes)
            val settlement = (range.to - 30.minutes).settlement
            electricImporters.importActualGeneration(settlement.date, settlement.period)
          case None =>
            // Nothing currently to import. Do nothing
            DBIOAction.successful(())
        }
      }
      log.info("ImportControl: actualGeneration starting")
      db.executeAndWait(qImport.transactionally, timeout)
      log.info("ImportControl: actualGeneration complete")
    }

    // Import some data.
    // Call every 2.5 minutes, with a ~1 minute offset
    // Will always get the most recent data (up to 24 hours at a time) that is not yet downloaded
    // This method will block for DB and network things
    // If pastOnly is set then it will not attempt to load current data.
    def fuelInst(pastOnly: Boolean, timeout: Duration)(implicit ec: ExecutionContext): Unit = {
      val now: Instant = clock.nowUtc()

      val extremes = SimpleRangeOf(minFuelInst, now.alignTo(5.minutes))
      val qMissing: DBIO[Seq[RangeOf[Instant]]] =
        data.missingRawProgress(RawDataType.Electric.generationByFuelType, extremes)
      val qImport: DBIO[ImportResult] = qMissing.flatMap { missing: Seq[RangeOf[Instant]] =>
        val useRange = if (pastOnly) {
          missing.filter(_.to < extremes.to).lastOption
        } else {
          missing.lastOption
        }
        useRange match {
          case Some(range) =>
            // Import most recent data that isn't already imported
            assert((range.to - range.from) >= 5.minutes)
            val toTime = range.to
            val fromTime = Seq(range.from, range.to - 24.hours).max + 1.second
            electricImporters.importFuelInst(fromTime.toLocalDateTimeUtc, toTime.toLocalDateTimeUtc)
          case None =>
            // Nothing currently available to import. Do nothing
            DBIO.successful(ImportResult.None)
        }
      }
      log.info("ImportControl: fuelInst starting")
      val r: ImportResult = db.executeAndWait(qImport.transactionally, timeout)
      log.info(s"ImportControl: fuelInst import result '$r'")
      log.info("ImportControl: fuelInst complete")
    }

    // Import some data.
    // Call every 2 minutes, with a 1 minute offset (???)
    // Will attempt a now download every 2 minutes, otherwise will get historic data
    def freq(pastOnly: Boolean, timeout: Duration)(implicit ec: ExecutionContext): Unit = {
      val now: Instant = clock.nowUtc()

      val extremes = SimpleRangeOf(minFreq, now.alignTo(15.seconds))
      val qMissing: DBIO[Seq[RangeOf[Instant]]] =
        data.missingRawProgress(RawDataType.Electric.frequency, extremes)
      val qImport: DBIO[ImportResult] = qMissing.flatMap { missing: Seq[RangeOf[Instant]] =>
        val useRange = if (pastOnly) {
          missing.filter(_.to < extremes.to).lastOption
        } else {
          missing.lastOption
        }
        useRange match {
          case Some(range) =>
            // Import most recent data that isn't already imported
            assert((range.to - range.from) >= 15.seconds)
            val toTime = range.to
            val fromTime = Seq(range.from, range.to - 1.hour).max
            log.info(s"ImportControl: freq $fromTime -> $toTime")
            electricImporters.importFreq(fromTime.toLocalDateTimeUtc, toTime.toLocalDateTimeUtc)
          case None =>
            // Nothing currently available to import. Do nothing
            log.info("ImportControl: freq <nothing to do>")
            DBIO.successful(ImportResult.None)
        }
      }
      log.info("ImportControl: freq starting")
      val r: ImportResult = db.executeAndWait(qImport.transactionally, timeout)
      log.info(s"ImportControl: freq import result '$r'")
      log.info("ImportControl: freq complete")
    }

  }

}
