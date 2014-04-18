package org.ukenergywatch.www.views

import scalatags._
import scalatags.all._

case class Layout(title: String, content: Node)

object Layout {

  def view(data: Layout): Node = {
    html(
      head(
        Tags2.title(s"UK Energy Watch - ${data.title}"),
        script(src := "http://d3js.org/d3.v3.min.js"),
        script(src := "/js/wwwjs-preopt.js")
      ),
      body(
        h1("UK Energy Watch"),
        data.content
      )
    )
  }

}

object Index {

  def render(): Node = {
    view()
  }

  private def view(): Node = {
    val frag =
      div(
        p(id := "a", "Hello world!"),
        script("Index().main()")
      )
    Layout.view(Layout("Home", frag))
  }

}