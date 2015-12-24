package org.ukenergywatch.importers

import org.ukenergywatch.utils.ClockComponent
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
  this: DbComponent with DataComponent with ElectricImportersComponent with ClockComponent =>

  lazy val importControl = new ImportControl

  class ImportControl {
    import db.driver.api._

    val minActualGeneration = LocalDate.of(2015, 1, 1).toInstant
    val minFuelInst = LocalDate.of(2015, 1, 1).toInstant

    // Import some data
    // Call every five minutes, with a ~1 minute offset
    // Always gets the latest data if possible
    // Otherwise gets past data, back to some fixed point
    // This method will block for DB and network things
    def actualGeneration(timeout: Duration)(implicit ec: ExecutionContext): Unit = {
      val now: Instant = clock.nowUtc()

      val extremes = SimpleRangeOf(minActualGeneration, now.alignTo(30.minutes))
      val qMissing: DBIO[Seq[RangeOf[Instant]]] =
        data.missingRawProgress(RawDataType.Electric.actualGeneration, extremes)
      val qImport = qMissing.flatMap { missing: Seq[RangeOf[Instant]] =>
        missing.lastOption match {
          case Some(range) =>
            // Import most recent data that isn't already imported
            assert((range.to - range.from) >= 30.minutes)
            val settlement = (range.to - 30.minutes).settlement
            electricImporters.importActualGeneration(settlement.date, settlement.period)
          case None =>
            // Nothing currently to import. Do nothing
            DBIOAction.successful(())
        }
      }.transactionally
      db.executeAndWait(qImport, timeout)
    }

    // Import some data.
    // Call every 2.5 minutes, with a ~1 minute offset
    // Will always get the most recent data (up to 24 hours at a time) that is not yet downloaded
    // This method will block for DB and network things
    def fuelInst(timeout: Duration)(implicit ec: ExecutionContext): Unit = {
      val now: Instant = clock.nowUtc()

      val extremes = SimpleRangeOf(minFuelInst, now.alignTo(5.minutes))
      val qMissing: DBIO[Seq[RangeOf[Instant]]] =
        data.missingRawProgress(RawDataType.Electric.generationByFuelType, extremes)
      val qImport = qMissing.flatMap { missing: Seq[RangeOf[Instant]] =>
        missing.lastOption match {
          case Some(range) =>
            // Import most recent data this isn't already imported
            assert((range.to - range.from) >= 5.minutes)
            val toTime = range.to
            val fromTime = Seq(range.from, range.to - 24.hours).max + 1.second
            electricImporters.importFuelInst(fromTime.toLocalDateTimeUtc, toTime.toLocalDateTimeUtc)
          case None =>
            // Nothing currently available to import. Do nothing
            DBIOAction.successful(())
        }
      }
      db.executeAndWait(qImport.transactionally, timeout)
    }

    def freq()(implicit ec: ExecutionContext): Unit = {
      ???
    }

  }

}
