package org.ukenergywatch.importer

import java.net.URL

trait HttpFetcherComp {

  def httpFetcher: HttpFetcher

  trait HttpFetcher {

    def fetch(url: URL, body: Option[String] = None, headers: Map[String, String] = Map()): Array[Byte]

  }

}

trait RealHttpFetcherComp extends HttpFetcherComp {
  lazy val httpFetcher: HttpFetcher = RealHttpFetcher

  object RealHttpFetcher extends HttpFetcher {
    import java.io.ByteArrayOutputStream
    import java.net.HttpURLConnection
    import java.util.zip.GZIPInputStream

    def fetch(url: URL, body: Option[String], headers: Map[String, String]): Array[Byte] = {
      val connection = url.openConnection().asInstanceOf[HttpURLConnection]
      connection.setDoOutput(true)
      for ((key, value) <- headers) {
        connection.setRequestProperty(key, value)
      }
      for (body <- body) {
        connection.setRequestMethod("POST")
        connection.getOutputStream.write(body.getBytes("UTF-8"))
      }
      connection.connect()
      val urlIs = Option(connection.getHeaderField("Content-Encoding")).map(_.toLowerCase) match {
        case Some(enc) if enc == "gzip" => new GZIPInputStream(connection.getInputStream)
        case None => connection.getInputStream
        case enc => throw new Exception(s"Cannot handle encoding: '$enc'")
      }
      val os = new ByteArrayOutputStream()
      val buffer = new Array[Byte](10000)
      var len = urlIs.read(buffer)
      while (len != -1) {
        os.write(buffer, 0, len)
        len = urlIs.read(buffer)
      }
      urlIs.close()
      os.toByteArray()
    }

  }

}