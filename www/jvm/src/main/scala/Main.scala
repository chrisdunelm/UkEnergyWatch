package org.ukenergywatch.www

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.webapp.WebAppContext
import org.eclipse.jetty.servlet.DefaultServlet
import org.scalatra.servlet.ScalatraListener
import io.prometheus.client.exporter.MetricsServlet

// TODO: Move this, make it actually do something
import org.scalatra.ScalatraServlet
object IndexServlet extends ScalatraServlet {
  get("/") {
    "Hello world!"
  }
}

object Main {

  object Metrics {
    import io.prometheus.client.Gauge
    val up = Gauge.build.name("up").help("Is this task up").register
  }

  def main(args: Array[String]): Unit = {
    println("www main")

    Metrics.up.set(1.0)

    // TODO: Componentise this!
    val server = new Server(8080)
    val context = new WebAppContext
    context.setContextPath("/")
    context.setResourceBase("src/main/webapp")
    context.addEventListener(new ScalatraListener)
    context.addServlet(classOf[MetricsServlet], "/metrics")
    context.addServlet(classOf[DefaultServlet], "/")
    server.setHandler(context)

    server.start()
    println("Press enter to exit")
    scala.io.StdIn.readLine()
    server.stop()
  }

}
