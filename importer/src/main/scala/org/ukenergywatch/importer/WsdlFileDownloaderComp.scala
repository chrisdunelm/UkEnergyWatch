/*package org.ukenergywatch.importer

trait WsdlFileDownloaderComp

trait WsdlHttpClientComp extends scalaxb.HttpClients {

  def httpClient = Client

  object Client extends HttpClient {
    import java.net._
    import java.io._
    def request(in: String, address: URI, headers: Map[String, String]): String = {
      val url = new URL(address.toString)
      val conn = url.openConnection()
      conn.setDoOutput(true)
      for ((key, value) <- headers) {
        conn.addRequestProperty(key, value)
      }
      val os = conn.getOutputStream()
      os.write(in.getBytes("UTF-8"))
      val br = new BufferedReader(new InputStreamReader(conn.getInputStream()))
      var res = ""
      var line = ""
      while ({ line = br.readLine(); line != null }) {
        res += line
      }
      res
    }
  }


}
*/