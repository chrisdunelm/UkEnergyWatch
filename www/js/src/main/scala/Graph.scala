package org.ukenergywatch.www

import scala.scalajs.js.annotation.JSExport

@JSExport
object Graph {

  @JSExport
  def init(containerId: String): Unit = {
    println(s"Graph.init: '$containerId'")
  }

}
