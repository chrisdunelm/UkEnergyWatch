package org.ukenergywatch.www

import org.scalatra._
import scalatags._
import scalatags.all._
import org.scalajs.spickling._
import org.scalajs.spickling.playjson._

import org.ukenergywatch.www.views._
import org.ukenergywatch.wwwcommon._

object UkewServlet extends ScalatraServlet {

  Pickling.register()

  private def output(node: Node): ActionResult = {
    contentType = "text/html"
    Ok(s"<!DOCTYPE HTML>\n$node")
  }

  private def ajax[T](data: T): ActionResult = {
    contentType = "application/json"
    Ok(PicklerRegistry.pickle(data))
  }

  val index = get("/") { output(Index.render()) }

  get("/a/index") { ajax(Index.getUpdate()) }

  notFound {
    serveStaticResource() getOrElse resourceNotFound()
  }

}
