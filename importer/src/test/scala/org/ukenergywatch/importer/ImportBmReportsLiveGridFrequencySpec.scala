package org.ukenergywatch.importer

import org.scalatest._
import org.joda.time._

class ImportBmReportsLiveGridFrequencySpec extends TestBase {

  import TestImporter.dal
  import TestImporter.dal._
  import TestImporter.dal.profile.simple._
  import TestImporter.Implicits._

  "Import BmReports live GridFrequency" should "import initial data" in prepare { implicit session =>
    val s = """
<?xml version="1.0"?>
<ROLLING_SYSTEM_FREQUENCY>
 <ST ST="2014-04-15 15:31:00" VAL="49.974"></ST>
 <ST ST="2014-04-15 15:31:15" VAL="49.975"></ST>
</ROLLING_SYSTEM_FREQUENCY>
      """.trim

    TestImporter.clock.set(new DateTime(2014, 4, 15, 6, 2, DateTimeZone.UTC))
    TestImporter.httpFetcher.set("http://www.bmreports.com/bsp/additional/saveoutput.php?element=rollingfrequency&output=XML", s)
    TestImporter.run(Importer.ImportLiveGridFrequency)

    GridFrequenciesLive.sortBy(_.endTime).list shouldBe List(
      GridFrequency(t(2014, 4, 15, 15, 31, 0), 49.974f),
      GridFrequency(t(2014, 4, 15, 15, 31, 15), 49.975f)
    )
  }

}