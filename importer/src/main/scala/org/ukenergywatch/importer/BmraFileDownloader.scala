package org.ukenergywatch.importer

import org.joda.time.ReadableInstant
import org.ukenergywatch.utils.JodaTimeExtensions._
import org.ukenergywatch.utils.Slogger
import java.net.URL

trait BmraFileDownloaderComp {

  def bmraFileDownloader: BmraFileDownloader

  trait BmraFileDownloader {

    /** fromTime must be the beginning of the half-hour file to get */
    def getHalfHour(fromTime: ReadableInstant): Iterator[String]

    /** date must be a date (time ignored) of the file to get */
    def getDay(date: ReadableInstant): Iterator[String]

  }

}

trait HttpBmraFileDownloaderComp extends BmraFileDownloaderComp {
  this: HttpFetcherComp with ConfigComp =>

  lazy val bmraFileDownloader: BmraFileDownloader = HttpBmraFileDownloader

  object HttpBmraFileDownloader extends BmraFileDownloader with Slogger {
    import java.io.ByteArrayInputStream
    import java.util.zip.GZIPInputStream

    private val key = config.getString("elexonKey").getOrElse("")

    private def makeUrl(filename: String): URL =
      new URL(s"https://downloads.elexonportal.co.uk/bmradataarchive/download?key=$key&filename=$filename")

    private def getFile(url: URL): Iterator[String] = {
      log.info(s"Downloading file: '$url'")
      val compressedBytes = httpFetcher.fetch(url)
      val is = new GZIPInputStream(new ByteArrayInputStream(compressedBytes))
      io.Source.fromInputStream(is).getLines
    }

    private def pad(i: Int): String = i.toString.reverse.padTo(2, '0').reverse

    def getHalfHour(fromTime: ReadableInstant): Iterator[String] = {
      // filename=tib_messages_hh.2014-03-03.17.00-17.30.gz
      val t0 = fromTime
      val t1 = t0 + 30.minutes
      val filename = s"tib_messages_hh.${t0.year}-${pad(t0.month)}-${pad(t0.day)}.${pad(t0.hour)}.${pad(t0.minute)}-${pad(t1.hour)}.${pad(t1.minute)}.gz"
      getFile(makeUrl(filename))
    }

    def getDay(date: ReadableInstant): Iterator[String] = {
      // filename=tib_messages.2014-03-01.gz
      val filename = s"tib_messages.${date.year}-${pad(date.month)}-${pad(date.day)}.gz"
      getFile(makeUrl(filename))
    }

  }

}
