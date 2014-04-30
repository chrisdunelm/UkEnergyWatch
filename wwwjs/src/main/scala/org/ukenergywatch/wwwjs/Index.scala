package org.ukenergywatch.wwwjs

import scala.scalajs.js
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

    dom.window.setTimeout(updateInfo, 5000)

  }

}
