package org.ukenergywatch.importer

import org.scalatest._
import org.joda.time._

class ImportBmReportsLiveGenByFuelSpec extends TestBase {

  import TestImporter.dal
  import TestImporter.dal._
  import TestImporter.dal.profile.simple._
  import TestImporter.Implicits._

  "Import BmReports live GenByFuel" should "import initial data" in prepare { implicit session =>
    val s = """
<GENERATION_BY_FUEL_TYPE_TABLE>
 <INST AT="2014-04-15 06:00:00" TOTAL="32970">
  <FUEL TYPE="CCGT" IC="N" VAL="6131" PCT="18.6"></FUEL>
  <FUEL TYPE="OCGT" IC="N" VAL="0" PCT="0.0"></FUEL>
  <FUEL TYPE="OIL" IC="N" VAL="0" PCT="0.0"></FUEL>
  <FUEL TYPE="COAL" IC="N" VAL="13633" PCT="41.3"></FUEL>
 </INST>
 <HH SD="2014-04-15" SP="14" AT="06:30-07:00" TOTAL="31687">
  <FUEL TYPE="CCGT" IC="N" VAL="5139" PCT="16.2"></FUEL>
 </HH>
 <LAST24H FROM_SD="2014-04-14" FROM_SP="15" AT="07:00-07:00" TOTAL="828466">
  <FUEL TYPE="CCGT" IC="N" VAL="152930" PCT="18.5"></FUEL>
 </LAST24H>
 <LAST_UPDATED AT="2014-04-15 06:00:00"></LAST_UPDATED>
</GENERATION_BY_FUEL_TYPE_TABLE>
      """.trim

    TestImporter.clock.set(new DateTime(2014, 4, 15, 6, 2, DateTimeZone.UTC))
    TestImporter.httpFetcher.set("http://www.bmreports.com/bsp/additional/saveoutput.php?element=generationbyfueltypetable&output=XML", s)
    TestImporter.run(Importer.ImportLiveGenByFuel)

    val t0 = t(2014, 4, 15, 5, 55)
    val t1 = t(2014, 4, 15, 6, 0)
    GenByFuelsLive.list.id0.toSet shouldBe Set(
      GenByFuel("CCGT", t0, t1, 6131),
      GenByFuel("OCGT", t0, t1, 0),
      GenByFuel("OIL", t0, t1, 0),
      GenByFuel("COAL", t0, t1, 13633)
    )
  }

  it should "not import if too soon" in prepare { implicit session =>
    
  }

}
