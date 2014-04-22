package org.ukenergywatch.importer

import org.scalatest._
import org.joda.time._

class ImportGasFlowSpec extends TestBase {

  import TestImporter.dal
  import TestImporter.dal._
  import TestImporter.dal.profile.simple._
  import TestImporter.Implicits._

  "GetLatestTime" should "return the correct time" in prepare { implicit session =>
    val s = """
<?xml version="1.0" encoding="utf-8"?>
<soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema">
<soap:Body>
<GetLatestPublicationTimeResponse xmlns="http://www.NationalGrid.com/EDP/UI/">
<GetLatestPublicationTimeResult>2014-04-22T22:47:02</GetLatestPublicationTimeResult>
</GetLatestPublicationTimeResponse>
</soap:Body>
</soap:Envelope>
      """.trim

    TestImporter.clock.set(new DateTime(2014, 3, 3, 22, 15, DateTimeZone.UTC))
    TestImporter.httpFetcher.set("http://energywatch.natgrid.co.uk/EDP-PublicUI/PublicPI/InstantaneousFlowWebService.asmx", s)
    TestImporter.run(Importer.ImportGas)

    Downloads.list.id0 shouldBe List(Download(Downloads.TYPE_BMRA, t(2014, 3, 3, 21, 30), t(2014, 3, 3, 22, 00)))
    BmUnitFpns.sortBy(_.id).list.id0 shouldBe List(
      BmUnitFpn("T_BARKB2", t(2014, 3, 3, 22, 30), 0, t(2014, 3, 3, 23, 0), 0),
      BmUnitFpn("E_BETHW-1", t(2014, 3, 3, 22, 30), 2, t(2014, 3, 3, 22, 31), 3),
      BmUnitFpn("E_BETHW-1", t(2014, 3, 3, 22, 31), 3, t(2014, 3, 3, 23, 0), 3),
      BmUnitFpn("T_BLLA-1", t(2014, 3, 3, 22, 30), 4, t(2014, 3, 3, 22, 45), 4),
      BmUnitFpn("T_BLLA-1", t(2014, 3, 3, 22, 45), 5, t(2014, 3, 3, 23, 0), 5)
    )
  }

}