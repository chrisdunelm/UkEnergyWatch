package org.ukenergywatch.www

import scala.scalajs.js.annotation.JSExport

// From here will remain in this file

@JSExport
object ElectricSummary {

  object Model extends ElectricSummaryModel with ModelUpdater[ElectricSummaryModel.Data]

  @JSExport
  def test(): Unit = {
    val data = new ElectricSummaryModel.Data(42, "forty-two")
    Model.update(data)
  }

}
