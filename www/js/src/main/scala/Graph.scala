package org.ukenergywatch.www

import scala.scalajs.js
import org.scalajs.dom
import org.scalajs.dom.{ html, raw }
import scalatags.JsDom.all._
import rx._
import rx.ops._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.annotation.JSExport
import org.ukenergywatch.www.TimeExtensions._

/*
Controls:

Add an 'axis'
Remove existing 'axis'

An 'axis' is some kind of plot type:
X/Y graph - x-axis must be time
  line graph (e.g. wind power)
  bar graph (column) (e.g. wind power)
bar chart (row) (e.g. kWh per generation type)
bar chart (column) (e.g. kWh per generation type)
pie chart (e.g. kWh per generation type)

For an 'axis':
Add data
Remove data
+edit

X/Y plot
Range, LH or RH axis

For each axis:
Select which time-range they are part of
All axis's that are part of the same time-range change together
Can link time-ranges (e.g. timerange1 = timerange2 - 1 year

How does it work
Use one big canvas
Configration things all opened as HTML elements, positioned at the correct place
Some kind of icon appears on the canvas to show where to click to open configuration
What about testual information? Maybe better to use lots of smaller canvas's?
Overlay text div (fixed-size) over canvas? This'll get a bit messy

Textual information:
Allow things like
Graph of all generation types, with selection line controlling where the text information is shown from?
 */

class Graph(container: html.Element) {
  implicit val scheduler = new DomScheduler

  val documentWidthAll = Var(dom.document.body.clientWidth)
  val documentWidth = documentWidthAll.debounce(500.millis.toConcurrent)
  dom.window.addEventListener("resize", (_: raw.Event) => {
    //documentWidthAll() = dom.document.body.clientWidth
    // Window width without scrollbar
    documentWidthAll() = dom.document.documentElement.clientWidth
  })

  val endDate = Var(Instant.utcNow())
  val duration = Var(24.hours)

  case class Layout(
    documentWidth: Int,
    endDate: Instant,
    duration: Duration
  )

  case class AxisInfo(

  )

  val layoutInfo = Rx {
    Layout(
      documentWidth = documentWidth(),
      endDate = endDate(),
      duration = duration()
    )
  }

  // Single axis, on a single canvas
  class Axis {
    val canvasEl = canvas().render
    private val ctx = canvasEl.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]

    private def render(): Unit = {
      canvasEl.width = documentWidth()
      canvasEl.height = 200
      canvasEl.style.width = s"${documentWidth()}px"
      canvasEl.style.height = "200px"
      ctx.fillStyle = "#00ff00"
      ctx.fillRect(0, 0, canvasEl.width, canvasEl.height)
      ctx.fillStyle = "#ff0000"
      ctx.fillRect(0, 0, canvasEl.width / 2, canvasEl.height / 2)
      println(s"width: ${documentWidth()}, height: 200")
    }

    Obs(layoutInfo) { render() }

  }

  def addAxis(): Axis = {
    val axis = new Axis()
    container.appendChild(axis.canvasEl)
    axis
  }
}

@JSExport
object Graph {
  var graph: Graph = null
  @JSExport
  def init(containerId: String): Unit = {
    println(s"Graph.init: '$containerId'")
    val container = dom.document.getElementById(containerId)
    graph = new Graph(container.asInstanceOf[html.Element])

    val axis0 = graph.addAxis()
  }

}
