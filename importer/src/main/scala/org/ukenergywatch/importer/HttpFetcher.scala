package org.ukenergywatch.importer

trait HttpFetcherComp {

  def httpFetcher: HttpFetcher

  trait HttpFetcher {

    def fetch(url: String): Array[Byte]

  }

}

trait RealHttpFetcherComp extends HttpFetcherComp {
  lazy val httpFetcher: HttpFetcher = RealHttpFetcher

  object RealHttpFetcher extends HttpFetcher {
    import java.io.ByteArrayOutputStream
    import java.net.URL

    def fetch(url: String): Array[Byte] = {
      val url2 = new URL(url)
      val connection = url2.openConnection()
      connection.connect()
      val urlIs = connection.getInputStream
      val os = new ByteArrayOutputStream()
      val buffer = new Array[Byte](10000)
//println("READING")
      var len = urlIs.read(buffer)
      while (len != -1) {
        os.write(buffer, 0, len)
//println("READING")
        len = urlIs.read(buffer)
      }
//println("DONE")
      urlIs.close()
      os.toByteArray()
/*
      val length = connection.getContentLength
      val data = new Array[Byte](length)
      try {
        val dis = new DataInputStream(urlIs)
        dis.readFully(data)
        data
      } finally {
        urlIs.close()
      }*/
    }

  }

}