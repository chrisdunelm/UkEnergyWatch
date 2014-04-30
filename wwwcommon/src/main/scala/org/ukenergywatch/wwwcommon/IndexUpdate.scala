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

  def htmlGenByFuel(data: IndexUpdate): Node = div(
    data.genByFuel.map(x => p(s"${x.fuel}: ${x.mw}")),
    p(s"(updated: ${data.genByFuelTime}")
  )

}
