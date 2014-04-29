package org.ukenergywatch.wwwcommon

import scalatags._
import scalatags.all._

case class IndexUpdate(
  gridFrequency: Double,
  gridFrequencyUpdate: String
)

object IndexUpdate {

  def htmlGridFrequency(data: IndexUpdate): Seq[Node] = Seq(
    p(s"Grid frequency: ${data.gridFrequency} (updated: ${data.gridFrequencyUpdate})")
  )

}