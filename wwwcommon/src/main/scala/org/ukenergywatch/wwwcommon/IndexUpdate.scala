package org.ukenergywatch.wwwcommon

import scalatags._
import scalatags.all._

case class GenByFuelUpdate(fuel: String, mw: Double)

case class IndexUpdate(
  gridFrequency: Double,
  gridFrequencyTime: String,
  genByFuel: Seq[GenByFuelUpdate],
  genByFuelTime: String
)

object IndexUpdate {

  def htmlGridFrequency(data: IndexUpdate): Seq[Node] = Seq(
    p(s"Grid frequency: ${data.gridFrequency} (updated: ${data.gridFrequencyTime})")
  )

  def htmlGenByFuel(data: IndexUpdate): Node = table("gen".cls)(
    tr("genhead".cls)(
      th("Fuel type"),
      th("Power (mw)")
    ),
    data.genByFuel.map { x =>
      tr("gen".cls)(
        td(x.fuel),
        td(x.mw)
      )
    },
    tr("genhead".cls)(
      th("Total"),
      th(data.genByFuel.map(x => x.mw).sum)
    )
  )

}
