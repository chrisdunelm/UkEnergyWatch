package org.ukenergywatch.utils

object Csv {

  // Remove starred lines. Final starred line contains comma-separated column names
  def starredColNames(lines: Seq[String]): (Map[String, Int], Seq[String]) = {
    val stars = lines.takeWhile(_.startsWith("*"))
    val colNames = stars.last.drop(1).split(',').zipWithIndex.toMap
    (colNames, lines.drop(stars.size))
  }

}

class Csv(colNames: Map[String, Int], lines: Seq[String]) {

  def extract[T](empty: T, cols: (String, (T, String) => T)*): Seq[T] = {
    lines.map { line =>
      val parts = line.split(',')
      cols.foldLeft(empty) { case (t, (colName, fn)) => fn(t, parts(colNames(colName))) }
    }
  }

}
