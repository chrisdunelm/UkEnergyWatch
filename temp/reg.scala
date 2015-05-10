
import scala.io.Source

val lines = Source.fromFile("./reg_bm_units.csv").getLines

val l = lines.drop(3).toList
//println(l)

case class Entry(
  bmUnitId: String,
  bmUnitName: String,
  partyName: String,
  partyId: String,
  bmuType: String,
  ngcBmuName: String,
  gspGroupId: String,
  gspGroupName: String,
  tradingUnitName: String,
  prodConsFlag: String,
  prodConsStatus: String,
  tlf: String,
  generationCapacity: String,
  dc: String
)

def toEntry(l: String): Entry = {
  def q(s: String): String = s.headOption match {
    case None => ""
    case Some('"') => s.drop(1).dropRight(1)
    case Some(_) => s
  }
  l.split(',') match {
    case Array(a, b, c, d, e, f, g, h, i, j, k, l, m, n, _*) =>
      Entry(q(a), q(b), q(c), q(d), q(e), q(f), q(g), q(h), q(i), q(j), q(k), q(l), q(m), q(n))
  }
}

val entries = l.map(x => toEntry(x))
//println(entries)

val tu = entries.groupBy(_.tradingUnitName)
for (z <- tu.filter(_._2.size < 10)) {
  println(s"${z._1} : ${z._2.size}")
  for (zz <- z._2) {
    println(s"  $zz")
  }
}
