package org.ukenergywatch.oldfueltype

import org.scalatest._

import org.ukenergywatch.utils._
import org.ukenergywatch.utils.JavaTimeExtensions._
import org.ukenergywatch.utils.StringExtensions._

class OldFuelTypeTest extends FunSuite with Matchers {

  val elexonData20151207184000 = """<?xml version="1.0"?>
<GENERATION_BY_FUEL_TYPE_TABLE>
<INST AT="2015-12-07 18:40:00" TOTAL="45104"><FUEL TYPE="CCGT" IC="N" VAL="14992" PCT="33.2"></FUEL><FUEL TYPE="OCGT" IC="N" VAL="0" PCT="0.0"></FUEL><FUEL TYPE="OIL" IC="N" VAL="0" PCT="0.0"></FUEL><FUEL TYPE="COAL" IC="N" VAL="10322" PCT="22.9"></FUEL><FUEL TYPE="NUCLEAR" IC="N" VAL="8767" PCT="19.4"></FUEL><FUEL TYPE="WIND" IC="N" VAL="5531" PCT="12.3"></FUEL><FUEL TYPE="PS" IC="N" VAL="331" PCT="0.7"></FUEL><FUEL TYPE="NPSHYD" IC="N" VAL="842" PCT="1.9"></FUEL><FUEL TYPE="OTHER" IC="N" VAL="2092" PCT="4.6"></FUEL><FUEL TYPE="INTFR" IC="Y" VAL="1246" PCT="2.8"></FUEL><FUEL TYPE="INTIRL" IC="Y" VAL="0" PCT="0.0"></FUEL><FUEL TYPE="INTNED" IC="Y" VAL="980" PCT="2.2"></FUEL><FUEL TYPE="INTEW" IC="Y" VAL="1" PCT="0.0"></FUEL></INST><HH SD="2015-12-07" SP="37" AT="18:00-18:30" TOTAL="45621"><FUEL TYPE="CCGT" IC="N" VAL="15425" PCT="33.8"></FUEL><FUEL TYPE="OCGT" IC="N" VAL="0" PCT="0.0"></FUEL><FUEL TYPE="OIL" IC="N" VAL="0" PCT="0.0"></FUEL><FUEL TYPE="COAL" IC="N" VAL="10479" PCT="23.0"></FUEL><FUEL TYPE="NUCLEAR" IC="N" VAL="8772" PCT="19.2"></FUEL><FUEL TYPE="WIND" IC="N" VAL="5436" PCT="11.9"></FUEL><FUEL TYPE="PS" IC="N" VAL="356" PCT="0.8"></FUEL><FUEL TYPE="NPSHYD" IC="N" VAL="836" PCT="1.8"></FUEL><FUEL TYPE="OTHER" IC="N" VAL="2093" PCT="4.6"></FUEL><FUEL TYPE="INTFR" IC="Y" VAL="1246" PCT="2.7"></FUEL><FUEL TYPE="INTIRL" IC="Y" VAL="0" PCT="0.0"></FUEL><FUEL TYPE="INTNED" IC="Y" VAL="978" PCT="2.1"></FUEL><FUEL TYPE="INTEW" IC="Y" VAL="0" PCT="0.0"></FUEL></HH><LAST24H FROM_SD="2015-12-06" FROM_SP="38" AT="18:30-18:30" TOTAL="862304"><FUEL TYPE="CCGT" IC="N" VAL="244031" PCT="28.3"></FUEL><FUEL TYPE="OCGT" IC="N" VAL="33" PCT="0.0"></FUEL><FUEL TYPE="OIL" IC="N" VAL="0" PCT="0.0"></FUEL><FUEL TYPE="COAL" IC="N" VAL="200245" PCT="23.2"></FUEL><FUEL TYPE="NUCLEAR" IC="N" VAL="209982" PCT="24.4"></FUEL><FUEL TYPE="WIND" IC="N" VAL="77891" PCT="9.0"></FUEL><FUEL TYPE="PS" IC="N" VAL="7512" PCT="0.9"></FUEL><FUEL TYPE="NPSHYD" IC="N" VAL="21825" PCT="2.5"></FUEL><FUEL TYPE="OTHER" IC="N" VAL="49786" PCT="5.8"></FUEL><FUEL TYPE="INTFR" IC="Y" VAL="26440" PCT="3.1"></FUEL><FUEL TYPE="INTIRL" IC="Y" VAL="2066" PCT="0.2"></FUEL><FUEL TYPE="INTNED" IC="Y" VAL="19993" PCT="2.3"></FUEL><FUEL TYPE="INTEW" IC="Y" VAL="2498" PCT="0.3"></FUEL></LAST24H><LAST_UPDATED AT="2015-12-07 18:40:00"></LAST_UPDATED></GENERATION_BY_FUEL_TYPE_TABLE>"""

  test("simple data collection") {
    object App extends OldFuelTypeComponent
        with DbMemoryComponent
        with SchedulerFakeComponent
        with DownloaderFakeComponent
        with ClockFakeComponent
        with LogMemoryComponent
    import App.db.driver.api._

    App.db.db.run(App.db.generationByFuelTypes.schema.create)

    App.downloader.content = Map(
      "https://downloads.elexonportal.co.uk/fuel/download/latest?key=key" -> elexonData20151207184000.toBytesUtf8
    )

    App.clock.fakeInstant = 6.minutesToInstant
    App.init("key", false, false)
    App.clock.fakeInstant += 2.minutes

    import scala.concurrent.Await
    import scala.concurrent.duration
    val fInsert = App.db.db.run(App.db.generationByFuelTypes.result)
    Await.ready(fInsert, 1.second.toConcurrent)

    // Check to see what's in the database
    val fData = App.db.db.run(App.db.generationByFuelTypes.result)
    val data = Await.result(fData, 1.second.toConcurrent)
    data.size shouldBe 12
    data.find(_.typ == "ccgt").get.value shouldBe 14992
    data.find(_.typ == "ocgt").get.value shouldBe 0
    data.find(_.typ == "oil").get.value shouldBe 0
    data.find(_.typ == "coal").get.value shouldBe 10322
    data.find(_.typ == "nuclear").get.value shouldBe 8767
    data.find(_.typ == "wind").get.value shouldBe 5531
    data.find(_.typ == "ps").get.value shouldBe 331
    data.find(_.typ == "npshyd").get.value shouldBe 842
    data.find(_.typ == "intfr").get.value shouldBe 1246
    data.find(_.typ == "intirl").get.value shouldBe 0
    data.find(_.typ == "intned").get.value shouldBe 980
    data.find(_.typ == "other").get.value shouldBe 2092 + 1
  }

}
