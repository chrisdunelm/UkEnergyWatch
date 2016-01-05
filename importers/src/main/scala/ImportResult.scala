package org.ukenergywatch.importers

import java.time.Instant
import org.ukenergywatch.utils.RangeOf

sealed trait ImportResult

object ImportResult {

  case class Ok(count: Int, times: RangeOf[Instant]) extends ImportResult {
    override def toString: String =
      s"Import Ok (count:$count ${times.from} -> ${times.to})"
  }

  case object Empty extends ImportResult {
    override def toString: String =
      "Import empty (count:0)"
  }

  case object None extends ImportResult {
    override def toString: String =
      "Import not performed"
  }

}
