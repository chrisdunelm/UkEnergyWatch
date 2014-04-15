package org.ukenergywatch.importer

import org.scalatest._
import org.joda.time._

class ImportBmraGenByFuelSpec extends TestBase {

  import TestImporter.dal
  import TestImporter.dal._
  import TestImporter.dal.profile.simple._
  import TestImporter.Implicits._

  "Import BMRA GenByFuel" should "collect initial current data" in prepare { implicit session =>
    val s = """
2013:10:31:09:30:25:GMT: subject=BMRA.SYSTEM.FUELINST, message={TP=2013:10:31:09:30:00:GMT,SD=2013:10:31:00:00:00:GMT,SP=19,TS=2013:10:31:09:25:00:GMT,FT=CCGT,FG=10105}
2013:10:31:09:30:25:GMT: subject=BMRA.SYSTEM.FUELINST, message={TP=2013:10:31:09:30:00:GMT,SD=2013:10:31:00:00:00:GMT,SP=19,TS=2013:10:31:09:25:00:GMT,FT=COAL,FG=16694}
      """.trim

    TestImporter.clock.set(new DateTime(2013, 10, 31, 10, 30, DateTimeZone.UTC))
    TestImporter.httpFetcher.setZ("https://downloads.elexonportal.co.uk/bmradataarchive/download?key=&filename=tib_messages_hh.2013-10-31.09.30-10.00.gz", s)
    TestImporter.run(Importer.ImportCurrent)

    GenByFuels.list.id0.toSet shouldBe Set(
      GenByFuel("CCGT", t(2013, 10, 31, 9, 25), t(2013, 10, 31, 9, 30), 10105),
      GenByFuel("COAL", t(2013, 10, 31, 9, 25), t(2013, 10, 31, 9, 30), 16694)
    )
  }

}
