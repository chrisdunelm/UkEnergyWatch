package org.ukenergywatch.www

import org.scalatra._
import scalatags._
import scalatags.all._

object UkewServlet extends ScalatraServlet {

  get("/") {
    val frag = html(
      head(
        Tags2.title("Hello")
      ),
      body(
        h1("Hello world from scalatags")
      )
    )
    contentType = "text/html"
    frag.toString
  }

}
