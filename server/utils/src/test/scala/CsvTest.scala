package org.ukenergywatch.utils

import org.scalatest._

class CsvTest extends FunSuite with Matchers {

  test("Csv extractor") {
    val lines = Seq(
      "*",
      "* whatever",
      "*",
      "*x,y,z",
      "a,b,c",
      "1,2,3"
    )
    val (colNames, dataLines) = Csv.starredColNames(lines)

    val csv = new Csv(colNames, dataLines)

    case class Xyz(x: String, y: String, z: String)
    val emptyXyz = Xyz("", "", "")

    val ex1 = csv.extract(emptyXyz,
      "x" -> ((xyz: Xyz, v: String) => xyz.copy(x = v)),
      "z" -> ((xyz: Xyz, v: String) => xyz.copy(z = v))
    ).toList
    ex1 should have size 2
    ex1(0) shouldBe Xyz("a", "", "c")
    ex1(1) shouldBe Xyz("1", "", "3")
  }

}
