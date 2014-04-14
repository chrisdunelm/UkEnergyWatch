package org.ukenergywatch.importer

import org.joda.time.ReadableInstant
import org.ukenergywatch.utils.JodaTimeExtensions._
import org.ukenergywatch.utils.Slogger
import scala.xml.Elem
import scala.xml.XML
import java.io.ByteArrayInputStream

trait BmReportsDownloaderComp {

  def bmReportsDownloader: BmReportsDownloader

  trait BmReportsDownloader {

    def getGenByFuelType(): Elem

    def getGridFrequency(): Elem

  }

}

trait HttpBmReportsDownloaderComp extends BmReportsDownloaderComp {
  this: HttpFetcherComp =>

  lazy val bmReportsDownloader: BmReportsDownloader = HttpBmReportsDownloader

  object HttpBmReportsDownloader extends BmReportsDownloader with Slogger {

    private def getXml(url: String): Elem = {
      val xmlBytes = httpFetcher.fetch(url)
      val is = new ByteArrayInputStream(xmlBytes)
      XML.load(is)
    }

    def getGenByFuelType(): Elem = {
      val url = "http://www.bmreports.com/bsp/additional/saveoutput.php?element=generationbyfueltypetablehistoric&output=XML"
      getXml(url)
    }

    def getGridFrequency(): Elem = {
      val url = "http://www.bmreports.com/bsp/additional/saveoutput.php?element=rollingfrequencyhistoric&output=XML"
      getXml(url)
    }

  }

}
