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
  def fuelInstOk_20151130_000001_20151201_000000 = get("/fuelInstOk_20151130_000001_20151201_000000.xml")
  def fuelInstOk_20151201_000001_20151201_000500 = get("/fuelInstOk_20151201_000001_20151201_000500.xml")

  def freqError_BadFormat = get("/freqError_BadFormat.xml")
  def freqOk_20151201_0000_0005 = get("/freqOk_20151201_0000_0005.xml")
  def freqOk_20151130_230000_20151201_000000 = get("/freqOk_20151130_230000_20151201_000000.xml")
  def freqOk_20151201_000000_20151201_000200 = get("/freqOk_20151201_000000_20151201_000200.xml")

}
