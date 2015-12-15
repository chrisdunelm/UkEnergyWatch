package org.ukenergywatch.importers

import org.ukenergywatch.utils.StreamExtensions._

object ElexonResponses {

  private def get(name: String): Array[Byte] =
    getClass.getResourceAsStream(name).toByteArray

  def b1610Error_BadKey = get("/B1610Error_BadKey.xml")
  def b1610Ok_20151201_1 = get("/B1610Ok_20151201_1.xml")
  def b1610Ok_20151201_2 = get("/B1610Ok_20151201_2.xml")

  def fuelInstError_BadFormat = get("/fuelInstError_BadFormat.xml")
  def fuelInstOk_20151201_00_01 = get("/fuelInstOk_20151201_00_01.xml")

}
