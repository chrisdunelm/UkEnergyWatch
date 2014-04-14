package org.ukenergywatch.wwwjs

import scala.scalajs.js

import js.annotation.JSExport
import js.Dynamic.global

@JSExport
object Index {

  @JSExport
  def main(): Unit = {
    val doc = global.document
    val p = doc.createElement("p")
    p.innerHTML = "Hello ScalaJS world!"
    doc.getElementById("a").appendChild(p)
  }

}
