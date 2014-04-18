package org.ukenergywatch.wwwjs

import scala.scalajs.js
import js.JSON

import js.annotation.JSExport
import org.scalajs.dom
import org.scalajs.dom.extensions._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

import org.scalajs.spickling._
import org.scalajs.spickling.jsany._
import org.ukenergywatch.wwwcommon.P1

import java.util.Random

import importedjs.d3

@JSExport
object Index {

  PicklerRegistry.register[P1]

  @JSExport
  def main(): Unit = {
    val doc = dom.document

    def show(s: String): Unit = {
      val p = doc.createElement("p")
      p.innerHTML = s
      doc.getElementById("a").appendChild(p)
    }

    show("Hello static ScalaJS world!")

    val rnd = new Random
    for (req <- Ajax.get(s"/json1?n=${rnd.nextInt}")) {
      show(req.responseText)
      // Unpickle
      val obj = PicklerRegistry.unpickle(JSON.parse(req.responseText): js.Any)
      show(obj.toString)

      d3.selectAll("p").style("color", "red")
    }
  }

}
