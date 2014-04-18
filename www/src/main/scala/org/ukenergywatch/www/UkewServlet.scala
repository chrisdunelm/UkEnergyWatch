package org.ukenergywatch.www

import org.scalatra._
import scalatags._
import scalatags.all._
import org.scalajs.spickling._
import org.scalajs.spickling.playjson._

import org.ukenergywatch.www.views._
import org.ukenergywatch.wwwcommon.P1

object UkewServlet extends ScalatraServlet {

  PicklerRegistry.register[P1]

  private def output(node: Node): ActionResult = {
    contentType = "text/html"
    Ok(s"<!DOCTYPE HTML>\n$node")
  }

  val index = get("/") { output(Index.render()) }

  get("/json1") {
    Ok {
      val pickle = PicklerRegistry.pickle(P1(1, "one"))
      pickle
    }
  }

  notFound {
    serveStaticResource() getOrElse resourceNotFound()
  }

}
