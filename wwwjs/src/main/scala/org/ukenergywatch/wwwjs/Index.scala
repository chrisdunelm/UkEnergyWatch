package org.ukenergywatch.wwwjs

import scala.scalajs.js

import js.annotation.JSExport
import js.Dynamic.global

@JSExport
object Index {

  @JSExport
  def main(): Unit = {
    val p = global.document.createElement("p")
    p.innerHTML = "Hello ScalaJS world!"
    global.document.getElementById("a").appendChild(p)
  }

}