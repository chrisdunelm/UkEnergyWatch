package org.ukenergywatch.www

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.webapp.WebAppContext
import org.eclipse.jetty.servlet.DefaultServlet
import org.scalatra.servlet.ScalatraListener
import org.ukenergywatch.utils.FlagsComponent

trait WwwServerComponent {

  def wwwServer: WwwServer

  trait WwwServer {
    def start(): Unit
    def stop(): Unit
  }

}

trait WwwServerScalatraComponent extends WwwServerComponent {
  this: WwwServerParamsComponent =>
  object wwwServer extends WwwServer {

    var server: Option[Server] = None

    def start(): Unit = {
      if (server.nonEmpty) {
        throw new Exception("Server already started, cannot start now.")
      }
      val svr = new Server(wwwServerParams.port)
      val context = new WebAppContext
      context.setContextPath("/")
      context.setResourceBase("/")
      context.addEventListener(new ScalatraListener)
      context.addServlet(Monitoring.servletClass, "/metrics")
      context.addServlet(classOf[DefaultServlet], "/")
      svr.setHandler(context)
      svr.start()
      server = Some(svr)
      Monitoring.up.set(1.0)
    }

    def stop(): Unit = {
      if (server.isEmpty) {
        throw new Exception("Server not started, cannot stop now.")
      }
      val srv = server.get
      srv.stop()
      server = None
      Monitoring.up.set(0.0)
    }

  }
}

trait WwwServerParamsComponent {
  def wwwServerParams: WwwServerParams
  trait WwwServerParams {
    def port: Int
  }
}

trait WwwServerFlagParamsComponent extends WwwServerParamsComponent {
  this: FlagsComponent =>
  object wwwServerParams extends WwwServerParams {
    object Flags extends FlagsBase {
      val wwwServerPort = flag[Int](name = "wwwServerPort", defaultValue = 8080)
    }
    def port: Int = Flags.wwwServerPort()
  }
  wwwServerParams.Flags // Early initialise
}
