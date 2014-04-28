package org.ukenergywatch.www.views

import scalatags._
import scalatags.all._

import org.ukenergywatch.db.DalComp
import org.joda.time.DateTime

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

  case class ViewData(gridFreq: Double, gridFreqUpdate: DateTime)

  def render(dal: DalComp#Dal): Node = {
    dal.database.withSession { implicit session =>
      val freq = dal.getLatestGridFrequency()
      val viewData = ViewData(freq.get.frequency, new DateTime(freq.get.endTime))
      view(viewData)
    }
  }

  private def view(viewData: ViewData): Node = {
    val frag =
      div(
        p(id := "a", "Hello world!"),
        p(s"Grid frequency: ${viewData.gridFreq}"),
        script("Index().main()")
      )
    Layout.view(Layout("Home", frag))
  }

}