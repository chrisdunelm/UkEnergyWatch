package org.ukenergywatch.importers

import org.ukenergywatch.utils.StreamExtensions._

object B1610Responses {

  def b1610Error_BadKey = this.getClass.getResourceAsStream("/B1610Error_BadKey.xml").toByteArray
  def b1610Ok_20151201_1 = this.getClass.getResourceAsStream("/B1610Ok_20151201_1.xml").toByteArray

}
