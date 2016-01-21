package org.ukenergywatch.www

import scala.scalajs.js.annotation.JSExport

import autowire._
import boopickle.DefaultBasic._
import scala.concurrent.Future
import scalajs.concurrent.JSExecutionContext.Implicits.runNow

@JSExport
object ElectricSummary {

  object Model extends ElectricSummaryModel with ModelUpdater[ElectricSummaryModel.Data]

  @JSExport
  def test(): Unit = {
    val data = ApiClient[Api].getElectricSummary().call()
    data.foreach { data =>
      Model.update(data)
    }
  }

}
