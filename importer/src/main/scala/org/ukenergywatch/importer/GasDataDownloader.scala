package org.ukenergywatch.importer

import org.joda.time.DateTime
import org.joda.time.DateTimeZone

trait GasDataDownloaderComp {

  def gasDataDownloader: GasDataDownloader

  trait GasDataDownloader {

    def getLatestPublicationTime(): DateTime
    def getInstantaneousFlowData(): Any

  }

}

trait WsdlGasDataDownloaderComp extends GasDataDownloaderComp {
  this: HttpFetcherComp =>

  lazy val gasDataDownloader = WsdlGasDataDownloader

  object WsdlGasDataDownloader extends GasDataDownloader {
    import scala.collection.JavaConversions._
    import java.util.zip.GZIPInputStream

    trait WsdlHttpClientComp extends scalaxb.HttpClients {
      def httpClient = Client
      object Client extends HttpClient {
        import java.net._
        import java.io._
        def request(in: String, address: URI, headers: Map[String, String]): String = {
println(in)
          val hs = headers + ("Accept-Encoding" -> "gzip")
          val bytes = httpFetcher.fetch(address.toURL, Some(in), hs)
          val s = new String(bytes, "UTF-8")
println(s)
s
        }
      }
    }

    private object XmlProtocol extends generated.XMLProtocol
    private object Services extends XmlProtocol.InstantaneousFlowWebServiceSoap12Bindings
      with scalaxb.SoapClients with WsdlHttpClientComp
    private val service = Services.service

    def getLatestPublicationTime(): DateTime = {
      val result = service.getLatestPublicationTime()
      val utcMillis = result.right.get.toGregorianCalendar.getTimeInMillis
      new DateTime(utcMillis, DateTimeZone.UTC)
    }

    def getInstantaneousFlowData(): Any = {
      service.getInstantaneousFlowData()
    }

  }

}
