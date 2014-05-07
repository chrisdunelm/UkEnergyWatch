package org.ukenergywatch.wwwjs

import scala.scalajs.js
import scala.scalajs.js._
import js.JSON

import js.annotation.JSExport
import org.scalajs.dom
import org.scalajs.dom.extensions._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

import org.scalajs.spickling._
import org.scalajs.spickling.jsany._
import org.ukenergywatch.wwwcommon._

import java.util.Random

import importedjs.d3
import importedjs.D3

@JSExport
object Index {

  Pickling.register()

  @JSExport
  def main(): Unit = {

    lazy val updateInfo: () => Unit = () => {
      for (req <- Ajax.get("/a/index")) {
        val indexUpdate = PicklerRegistry.unpickle(JSON.parse(req.responseText): js.Any).asInstanceOf[IndexUpdate]
        val html = IndexUpdate.htmlGridFrequency(indexUpdate)
        val d = dom.document.getElementById("freq")
        d.innerHTML = html.map(_.toString).mkString
        val html2 = IndexUpdate.htmlGenByFuel(indexUpdate)
        val d2 = dom.document.getElementById("genByFuel")
        d2.innerHTML = html2.toString
        dom.window.setTimeout(updateInfo, 5000)
      }
    }

    //dom.window.setTimeout(updateInfo, 5000)

    val d = dom.document.getElementById("d")
    d.innerHTML = "D3"
    val svgEl = d3.select("#s")
    val svgWidth = svgEl.style("width").dropRight(2).toInt
    val svgHeight = svgEl.style("height").dropRight(2).toInt
    println(s"SVG size: $svgWidth, $svgHeight")

    case class Margins(top: Int, right: Int, bottom: Int, left: Int)
    val margins = Margins(20, 20, 30, 50)
    val width = svgWidth - margins.left - margins.right
    val height = svgHeight - margins.top - margins.bottom

    val svg = svgEl.append("g")
      .attr("transform", s"translate(${margins.left},${margins.top})")

    val x = d3.time.scale.range(Array(0, width))
    val y = d3.scale.linear.range(Array(height, 0))
    val xAxis = d3.svg.axis.scale(x).orient("bottom")
    val yAxis = d3.svg.axis.scale(y).orient("left")

    case class Pt2(t: js.Date, y: Double)
    val data = Array(
      Pt2(new js.Date(2014, 1, 1), 1.0),
      Pt2(new js.Date(2014, 1, 2), 2.0),
      Pt2(new js.Date(2014, 1, 3), 4.0),
      Pt2(new js.Date(2014, 1, 4), 16.0)
    )
    x.domain(d3.extent(data, (d: Pt2) => d.t.asInstanceOf[js.Any]))
    y.domain(d3.extent(data, (d: Pt2) => d.y.asInstanceOf[js.Any]))

    svg.append("g")
      .attr("transform", s"translate(0, $height)")
      .call(xAxis)
    svg.append("g")
      .call(yAxis)
  }

}
