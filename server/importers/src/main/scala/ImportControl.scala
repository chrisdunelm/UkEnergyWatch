package org.ukenergywatch.importers

import org.ukenergywatch.utils.ClockComponent
import java.time.Instant

trait ImportControlComponent {
  this: ImportersComponent with ClockComponent =>

  lazy val importControl = new ImportControl

  class ImportControl {

    // Import some data, and generate aggregations
    def actualGeneration(): Unit = {
      //val now: Instant = clock.now()
    }

  }

}
