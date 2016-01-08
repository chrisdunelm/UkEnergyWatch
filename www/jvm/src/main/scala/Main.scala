package org.ukenergywatch.www

import org.ukenergywatch.utils.FlagsComponent

// TODO: Move this, make it actually do something
import org.scalatra.ScalatraServlet
import org.scalatra._
trait CommonServletComponent {

  object commonServlet extends ScalatraServlet
      with ElectricSummary
      with GraphTest
  {
    notFound {
      val path = s"/webapp${request.getRequestURI}"
      Option(getClass.getResourceAsStream(path)) match {
        case Some(stream) =>
          MimeType.fromFilename(path) match {
            case Some(mimeType) =>
              contentType = mimeType
              Ok(stream)
            case None =>
              throw new Exception(s"Don't know mime-type of '$path'")
          }
        case None => resourceNotFound()
      }
    }
    get("/") { "Hello World!" }
    get("/ElectricSummary") { contentType="text/html"; electricSummary.view() }
    get("/GraphTest") { contentType = "text/html"; Ok(graphTest.view()) }
  }

}

object Main {

  val app = {
    object App extends WwwServerScalatraComponent
        with WwwServerFlagParamsComponent
        with FlagsComponent
        with CommonServletComponent
    App
  }

  def main(args: Array[String]): Unit = {
    println("www main")

    app.wwwServer.start()

    println("In sbt, run using '~re-start'")
    println("Enter 'exit' and press enter to exit")
    var exit = false
    while (!exit) {
      exit = scala.io.StdIn.readLine() == "exit"
    }

    app.wwwServer.stop()
  }

}
