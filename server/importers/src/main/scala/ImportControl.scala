package org.ukenergywatch.importers

import org.ukenergywatch.utils.ClockComponent
import org.ukenergywatch.data.DataComponent
import org.ukenergywatch.db.DbComponent
import java.time.{ LocalDate, Instant }
import org.ukenergywatch.utils.JavaTimeExtensions._
import scala.concurrent.ExecutionContext
import org.ukenergywatch.utils.{ SimpleRangeOf, RangeOf }
import org.ukenergywatch.db.RawDataType
import slick.dbio.DBIOAction

trait ImportControlComponent {
  this: DbComponent with DataComponent with ImportersComponent with ClockComponent =>

  lazy val importControl = new ImportControl

  class ImportControl {
    import db.driver.api._

    val minActualGeneration = LocalDate.of(2015, 1, 1).toInstant

    // Import some data, and generate aggregations
    // Call every five minutes, with a 1 minute offset
    // Always gets the latest data if possible
    // Otherwise gets past data, back to some fixed point
    // This method will block for DB and network things
    def actualGeneration()(implicit ec: ExecutionContext): Unit = {
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
            importers.importActualGeneration(settlement.date, settlement.period)
          case None =>
            // Nothing currently to import. Do nothing
            DBIOAction.successful(())
        }
      }.transactionally
      val qAggregate =
        data.actualGenerationHourAggregatesFromRaw(limit = 1) >>
        data.actualGenerationSubAggregatesDay(limit = 1) >>
        data.actualGenerationSubAggregatesWeek(limit = 1) >>
        data.actualGenerationSubAggregatesMonth(limit = 1) >>
        data.actualGenerationSubAggregatesYear(limit = 1)
      // This executes the entire check-download-import pipeline
      val qAll = qImport >> qAggregate
      db.executeAndWait(qAll, 4.minutes)
    }

  }

}
