package org.ukenergywatch.importer

import org.scalatest._
import org.ukenergywatch.db.DalComp
import scala.collection.mutable
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream
import java.util.UUID
import org.ukenergywatch.utils._
import org.joda.time._

trait TestBase extends FlatSpec with Matchers {

  trait MemoryDalComp extends DalComp {
    val dal = new MemoryDal(UUID.randomUUID.toString)
    class MemoryDal(dbName: String) extends Dal {
      val profile = scala.slick.driver.H2Driver
      val database = profile.simple.Database.forURL(s"jdbc:h2:mem:$dbName;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
    }
  }

  trait FakeFetcherComp extends HttpFetcherComp {
    lazy val httpFetcher: FakeFetcher = new FakeFetcher
    class FakeFetcher extends HttpFetcher {
      import java.net.URL
      case class Serve(url: String, bodyContains: Option[String], data: Array[Byte])
      private val serves = mutable.MutableList[Serve]()
      def fetch(url: URL, body: Option[String], headers: Map[String, String]): Array[Byte] = {
        serves.find(x => x.url == url.toString && x.bodyContains.map(y => body.get.contains(y)).getOrElse(true)) match {
          case Some(v) => v.data
          case None => throw new Exception(s"Cannot server url '$url'")
        }
      }
      def set(url: String, value: String, bodyContains: Option[String] = None): Unit =
        serves += Serve(url, bodyContains, value.getBytes("UTF-8"))
      def setZ(url: String, value: String, bodyContains: Option[String] = None): Unit = {
        val bos = new ByteArrayOutputStream
        val gzos = new GZIPOutputStream(bos)
        gzos.write(value.getBytes("UTF-8"))
        gzos.close()
        serves += Serve(url, bodyContains, bos.toByteArray)
      }
    }
  }

  trait FakeConfigComp extends ConfigComp {
    lazy val config: FakeConfig = new FakeConfig
    class FakeConfig extends Config {
      val map = mutable.Map[String, String]()
      def getString(key: String): Option[String] = map.get(key)
      def set(key: String, value: String): Unit = map.update(key, value)
    }
  }

  object TestImporter extends RealImporter
    with HttpBmraFileDownloaderComp
    with HttpBmReportsDownloaderComp
    with WsdlGasDataDownloaderComp
    with FakeFetcherComp
    with MemoryDalComp
    with FakeClockComp
    with FakeConfigComp {

    object Implicits {
      import dal._

      implicit class RichDownload(val v: Download) {
        def id0: Download = v.copy(id = 0)
      }

      implicit class RichDownloadSeq(val vs: Seq[Download]) {
        def id0: Seq[Download] = vs.map(_.id0)
      }
  
      implicit class RichBmUnitFpn(val v: BmUnitFpn) {
        def id0: BmUnitFpn = v.copy(id = 0)
      }

      implicit class RichBmUnitFpnSeq(val vs: Seq[BmUnitFpn]) {
        def id0: Seq[BmUnitFpn] = vs.map(_.id0)
      }

      implicit class RichGenByFuel(val v: GenByFuel) {
        def id0: GenByFuel = v.copy(id = 0)
      }

      implicit class RichGenByFuelSeq(val vs: Seq[GenByFuel]) {
        def id0: Seq[GenByFuel] = vs.map(_.id0)
      }

      implicit class RichGasImport(val v: GasImport) {
        def id0: GasImport = v.copy(id = 0)
      }

      implicit class RichGasImportSeq(val vs: Seq[GasImport]) {
        def id0: Seq[GasImport] = vs.map(_.id0)
      }
    }

  }

  import TestImporter.dal
  import TestImporter.dal.profile.simple._

  def t(dt: DateTime): Int = (dt.getMillis / 1000L).toInt
  def t(year: Int, month: Int, day: Int, hour: Int, minute: Int): Int =
    t(new DateTime(year, month, day, hour, minute, DateTimeZone.UTC))
  def t(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int): Int =
    t(new DateTime(year, month, day, hour, minute, second, DateTimeZone.UTC))

  def prepare(fn: Session => Unit): Unit = {
    dal.database withSession { implicit session =>
      import TestImporter.dal.profile.simple._
      for (ddl <- dal.ddls) {
        //try { ddl.drop } catch { case e: Throwable => }
        ddl.create
      }
      try {
        fn(session)
      } finally {
        for (ddl <- dal.ddls) {
          ddl.drop
        }
      }
    }
  }

}
