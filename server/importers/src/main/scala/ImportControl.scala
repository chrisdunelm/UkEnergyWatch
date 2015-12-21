package org.ukenergywatch.importers

import org.ukenergywatch.utils.ClockComponent
import org.ukenergywatch.data.DataComponent
import java.time.{ LocalDate, Instant }
import org.ukenergywatch.utils.JavaTimeExtensions._
import scala.concurrent.ExecutionContext
import org.ukenergywatch.utils.{ SimpleRangeOf, RangeOf }
import org.ukenergywatch.db.RawDataType
import slick.dbio.{ DBIO, DBIOAction }

trait ImportControlComponent {
  this: DataComponent with ImportersComponent with ClockComponent =>

  lazy val importControl = new ImportControl

  class ImportControl {

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
      qMissing.flatMap { missing: Seq[RangeOf[Instant]] =>
        missing.lastOption match {
          case Some(range) =>
            assert((range.to - range.from) >= 30.minutes)
            //val nowLondon = (range.to - 30.minutes).toLondon
            //val settlementDate = nowLondon.toLocalDate
            //val settlementPeriod = nowLondon.settlementPeriod
            ???
          case None =>
            // Nothing currently to import. Do nothing
            DBIOAction.successful(())
        }
      }
    }

  }

}
