package org.ukenergywatch.importer

import org.scalatest._
import org.joda.time._

class ImportBmraGridFrequencySpec extends TestBase {

  import TestImporter.dal
  import TestImporter.dal._
  import TestImporter.dal.profile.simple._
  import TestImporter.Implicits._

  "Import BMRA grid frequency" should "import correctly" in prepare { implicit session =>
    val s = """
2013:10:31:09:30:09:GMT: subject=BMRA.SYSTEM.FREQ, message={TS=2013:10:31:09:28:00:GMT,SF=49.976}
2013:10:31:09:30:09:GMT: subject=BMRA.SYSTEM.FREQ, message={TS=2013:10:31:09:28:15:GMT,SF=49.969}
2013:10:31:09:30:09:GMT: subject=BMRA.SYSTEM.FREQ, message={TS=2013:10:31:09:28:30:GMT,SF=49.966}
    """.trim

    TestImporter.clock.set(new DateTime(2013, 10, 31, 10, 30, DateTimeZone.UTC))
    TestImporter.httpFetcher.set("https://downloads.elexonportal.co.uk/bmradataarchive/download?key=&filename=tib_messages_hh.2013-10-31.09.30-10.00.gz", s)
    TestImporter.run(Importer.ImportCurrent)

    GridFrequencies.sortBy(_.endTime).list shouldBe List(
      GridFrequency(t(2013, 10, 31, 9, 28, 0), 49.976f),
      GridFrequency(t(2013, 10, 31, 9, 28, 15), 49.969f),
      GridFrequency(t(2013, 10, 31, 9, 28, 30), 49.966f)
    )
  }

}