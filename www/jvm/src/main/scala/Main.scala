package org.ukenergywatch.www

import org.ukenergywatch.utils.FlagsComponent
import org.ukenergywatch.utils.StreamExtensions._
import boopickle.Default._
import scala.concurrent.ExecutionContext.Implicits.global
import java.nio.ByteBuffer
import scala.concurrent.{ Future, Await }
import org.ukenergywatch.utils.JavaTimeExtensions._

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
    get("/ElectricSummary") { contentType = "text/html"; electricSummary.view() }
    get("/GraphTest") { contentType = "text/html"; Ok(graphTest.view()) }

    post("""^/api/(.*)""".r) {
      contentType = "application/octet-stream"
      val apiRoute = params("captures")
      val body = ByteBuffer.wrap(request.inputStream.toByteArray)
      val r: Future[Array[Byte]] = ApiRouter.route[Api](ApiImpl) {
        autowire.Core.Request(apiRoute.split('/'), Unpickle[Map[String, ByteBuffer]].fromBytes(body))
      }.map { buffer =>
        val data = Array.ofDim[Byte](buffer.remaining)
        buffer.get(data)
        data
      }
      val rValue: Array[Byte] = try { Await.result(r, 2.seconds.toConcurrent) } catch {
        case t: Throwable => t.printStackTrace(); throw t
      }
      Ok(rValue)
    }
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
