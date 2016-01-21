package org.ukenergywatch.www

import scala.scalajs.js

object Time {
  def utcNow(): UInstant = UInstant((js.Date.now() * 1000.0).toLong)
}
