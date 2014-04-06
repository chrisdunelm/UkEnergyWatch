package org.ukenergywatch.importer

import org.scalatest._
import org.joda.time._

class ImportBmraFpnSpec extends TestBase {

  import TestImporter.dal
  import TestImporter.dal._
  import TestImporter.dal.profile.simple._
  import TestImporter.Implicits._

  "Import BMRA FPN" should "collect initial current data" in prepare { implicit session =>
    val s = """
2014:03:03:21:31:39:GMT: subject=BMRA.BM.T_BARKB2.FPN, message={SD=2014:03:03:00:00:00:GMT,SP=46,NP=2,TS=2014:03:03:22:30:00:GMT,VP=0.0,TS=2014:03:03:23:00:00:GMT,VP=0.0}
2014:03:03:21:31:39:GMT: subject=BMRA.BM.E_BETHW-1.FPN, message={SD=2014:03:03:00:00:00:GMT,SP=46,NP=3,TS=2014:03:03:22:30:00:GMT,VP=2.0,TS=2014:03:03:22:31:00:GMT,VP=3.0,TS=2014:03:03:23:00:00:GMT,VP=3.0}
2014:03:03:21:31:39:GMT: subject=BMRA.BM.T_BLLA-1.FPN, message={SD=2014:03:03:00:00:00:GMT,SP=46,NP=4,TS=2014:03:03:22:30:00:GMT,VP=4.0,TS=2014:03:03:22:45:00:GMT,VP=4.0,TS=2014:03:03:22:45:00:GMT,VP=5.0,TS=2014:03:03:23:00:00:GMT,VP=5.0}
      """.trim

    TestImporter.clock.set(new DateTime(2014, 3, 3, 22, 15, DateTimeZone.UTC))
    TestImporter.httpFetcher.set("https://downloads.elexonportal.co.uk/bmradataarchive/download?key=&filename=tib_messages_hh.2014-03-03.21.30-22.00.gz", s)
    TestImporter.run(Importer.ImportCurrent)

    Downloads.list.id0 shouldBe List(Download(Downloads.TYPE_BMRA, t(2014, 3, 3, 21, 30), t(2014, 3, 3, 22, 00)))
    BmUnitFpns.sortBy(_.id).list.id0 shouldBe List(
      BmUnitFpn("T_BARKB2", t(2014, 3, 3, 22, 30), 0, t(2014, 3, 3, 23, 0), 0),
      BmUnitFpn("E_BETHW-1", t(2014, 3, 3, 22, 30), 2, t(2014, 3, 3, 22, 31), 3),
      BmUnitFpn("E_BETHW-1", t(2014, 3, 3, 22, 31), 3, t(2014, 3, 3, 23, 0), 3),
      BmUnitFpn("T_BLLA-1", t(2014, 3, 3, 22, 30), 4, t(2014, 3, 3, 22, 45), 4),
      BmUnitFpn("T_BLLA-1", t(2014, 3, 3, 22, 45), 5, t(2014, 3, 3, 23, 0), 5)
    )
  }

  it should "collect data from after the previous download when less than 48 hour interval" in prepare { implicit session =>
    val s = """
2014:03:03:22:01:39:GMT: subject=BMRA.BM.T_BARKB2.FPN, message={SD=2014:03:03:00:00:00:GMT,SP=46,NP=2,TS=2014:03:03:22:00:00:GMT,VP=0.0,TS=2014:03:03:22:30:00:GMT,VP=0.0}
      """.trim

    Downloads.mergeInsert(Download(Downloads.TYPE_BMRA, t(2014, 3, 3, 21, 30), t(2014, 3, 3, 22, 0)))

    TestImporter.clock.set(new DateTime(2014, 3, 4, 23, 0, DateTimeZone.UTC))
    TestImporter.httpFetcher.set("https://downloads.elexonportal.co.uk/bmradataarchive/download?key=&filename=tib_messages_hh.2014-03-03.22.00-22.30.gz", s)
    TestImporter.run(Importer.ImportCurrent)

    Downloads.list.id0 shouldBe List(Download(Downloads.TYPE_BMRA, t(2014, 3, 3, 21, 30), t(2014, 3, 3, 22, 30)))
    BmUnitFpns.list.id0 shouldBe List(BmUnitFpn("T_BARKB2", t(2014, 3, 3, 22, 0), 0, t(2014, 3, 3, 22, 30), 0))
  }

  it should "collect data from now when previous download is more than 48 hours old" in prepare { implicit session =>
    val s = """
2014:03:03:21:01:39:GMT: subject=BMRA.BM.T_BARKB2.FPN, message={SD=2014:03:03:00:00:00:GMT,SP=46,NP=2,TS=2014:03:03:22:00:00:GMT,VP=0.0,TS=2014:03:03:22:30:00:GMT,VP=0.0}
      """.trim

    Downloads.mergeInsert(Download(Downloads.TYPE_BMRA, t(2014, 3, 1, 21, 30), t(2014, 3, 1, 22, 0)))

    TestImporter.clock.set(new DateTime(2014, 3, 3, 22, 0, DateTimeZone.UTC))
    TestImporter.httpFetcher.set("https://downloads.elexonportal.co.uk/bmradataarchive/download?key=&filename=tib_messages_hh.2014-03-03.21.00-21.30.gz", s)
    TestImporter.run(Importer.ImportCurrent)

    Downloads.list.id0 shouldBe List(
      Download(Downloads.TYPE_BMRA, t(2014, 3, 1, 21, 30), t(2014, 3, 1, 22, 0)),
      Download(Downloads.TYPE_BMRA, t(2014, 3, 3, 21, 0), t(2014, 3, 3, 21, 30))
    )
    BmUnitFpns.list.id0 shouldBe List(BmUnitFpn("T_BARKB2", t(2014, 3, 3, 22, 0), 0, t(2014, 3, 3, 22, 30), 0))
  }

  it should "do nothing when there is no more data to load yet" in prepare { implicit session =>
    Downloads.mergeInsert(Download(Downloads.TYPE_BMRA, t(2014, 3, 1, 21, 30), t(2014, 3, 1, 22, 0)))

    TestImporter.clock.set(new DateTime(2014, 3, 2, 22, 15, DateTimeZone.UTC))
    TestImporter.run(Importer.ImportCurrent)

    Downloads.list.id0 shouldBe List(Download(Downloads.TYPE_BMRA, t(2014, 3, 1, 21, 30), t(2014, 3, 1, 22, 0)))
    BmUnitFpns.list.id0 shouldBe List()
  }

  it should "do nothing when it's too soon to have any more data" in prepare { implicit session =>
    val s = """
2014:03:03:22:01:39:GMT: subject=BMRA.BM.T_BARKB2.FPN, message={SD=2014:03:03:00:00:00:GMT,SP=46,NP=2,TS=2014:03:03:22:30:00:GMT,VP=0.0,TS=2014:03:03:23:00:00:GMT,VP=0.0}
      """.trim

    Downloads.mergeInsert(Download(Downloads.TYPE_BMRA, t(2014, 3, 3, 21, 30), t(2014, 3, 3, 22, 0)))

    TestImporter.httpFetcher.set("https://downloads.elexonportal.co.uk/bmradataarchive/download?key=&filename=tib_messages_hh.2014-03-03.22.00-22.30.gz", s)
    TestImporter.clock.set(new DateTime(2014, 3, 3, 22, 30, DateTimeZone.UTC))
    TestImporter.run(Importer.ImportCurrent)

    Downloads.list.id0 shouldBe List(Download(Downloads.TYPE_BMRA, t(2014, 3, 3, 21, 30), t(2014, 3, 3, 22, 0)))
    BmUnitFpns.list.id0 shouldBe List()
  }

  it should "load no old files if no current files exist" in prepare { implicit session =>
    TestImporter.clock.set(new DateTime(2014, 2, 20, 10, 15, DateTimeZone.UTC))
    TestImporter.run(Importer.ImportOld)

    Downloads.list shouldBe List()
    BmUnitFpns.list shouldBe List()
  }

  it should "load correct old file, and only import relevant data, when not aligned" in prepare { implicit session =>
    val s = """
2014:03:03:21:21:39:GMT: subject=BMRA.BM.T_BARKB1.FPN, message={SD=2014:03:03:00:00:00:GMT,SP=46,NP=2,TS=2014:03:03:21:30:00:GMT,VP=1.0,TS=2014:03:03:22:00:00:GMT,VP=1.0}
2014:03:03:21:31:39:GMT: subject=BMRA.BM.T_BARKB2.FPN, message={SD=2014:03:03:00:00:00:GMT,SP=46,NP=2,TS=2014:03:03:22:30:00:GMT,VP=0.0,TS=2014:03:03:23:00:00:GMT,VP=0.0}
    """.trim

    Downloads.mergeInsert(Download(Downloads.TYPE_BMRA, t(2014, 3, 3, 21, 30), t(2014, 3, 5, 0, 0)))

    TestImporter.httpFetcher.set("https://downloads.elexonportal.co.uk/bmradataarchive/download?key=&filename=tib_messages.2014-03-03.gz", s)
    TestImporter.run(Importer.ImportOld)

    Downloads.list.id0 shouldBe List(Download(Downloads.TYPE_BMRA, t(2014, 3, 3, 0, 0), t(2014, 3, 5, 0, 0)))
    BmUnitFpns.list.id0 shouldBe List(BmUnitFpn("T_BARKB1", t(2014, 3, 3, 21, 30), 1, t(2014, 3, 3, 22, 0), 1))
  }

  it should "load correct old file when aligned" in prepare { implicit session =>
    val s = """
2014:03:02:21:21:39:GMT: subject=BMRA.BM.T_BARKB1.FPN, message={SD=2014:03:02:00:00:00:GMT,SP=46,NP=2,TS=2014:03:02:21:30:00:GMT,VP=1.0,TS=2014:03:02:22:00:00:GMT,VP=1.0}
    """.trim

    Downloads.mergeInsert(Download(Downloads.TYPE_BMRA, t(2014, 3, 3, 0, 0), t(2014, 3, 5, 0, 0)))

    TestImporter.httpFetcher.set("https://downloads.elexonportal.co.uk/bmradataarchive/download?key=&filename=tib_messages.2014-03-02.gz", s)
    TestImporter.run(Importer.ImportOld)

    Downloads.list.id0 shouldBe List(Download(Downloads.TYPE_BMRA, t(2014, 3, 2, 0, 0), t(2014, 3, 5, 0, 0)))
    BmUnitFpns.list.id0 shouldBe List(BmUnitFpn("T_BARKB1", t(2014, 3, 2, 21, 30), 1, t(2014, 3, 2, 22, 0), 1))
  }

}
