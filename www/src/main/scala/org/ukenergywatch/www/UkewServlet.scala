package org.ukenergywatch.www

import org.scalatra._
import scalatags._
import scalatags.all._

import org.ukenergywatch.www.views._

object UkewServlet extends ScalatraServlet {

  private def output(node: Node): ActionResult = {
    contentType = "text/html"
    Ok(s"<!DOCTYPE HTML>\n$node")
  }

  val index = get("/") { output(Index.render()) }

  notFound {
    serveStaticResource() getOrElse resourceNotFound()
  }

}
