package org.ukenergywatch.www

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.DefaultServlet
import org.eclipse.jetty.webapp.WebAppContext
import org.scalatra.servlet.ScalatraListener
import org.ukenergywatch.utils.Slogger

object Main extends Slogger {

  def main(args: Array[String]) {
    Flags.parse(args)
    println("UkEnergyWatch website starting")
    log.info("UkEnergywatch website starting")

    val context = new WebAppContext
    context.setInitParameter("org.eclipse.jetty.servlet.Default.useFileMappedBuffer", "false")
    context.setContextPath("/")
    context.setResourceBase("target/scala-2.10/classes/")
    context.addEventListener(new ScalatraListener)
    context.addServlet(classOf[DefaultServlet], "/")

    val server = new Server(Flags.port())
    server.setHandler(context)
    server.start()
    server.join()
  }

}
