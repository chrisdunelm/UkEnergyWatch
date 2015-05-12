
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
  gc: String,
  dc: String,
  calf: String,
  bmcaic: String,
  bmcaec: String,
  exemptExportFlag: String,
  baseTuFlag: String,
  fpnFlag: String,
  interconnectorId: String,
  effectiveFrom: String,
  manualCreditQualifyingFlag: String,
  creditQualifyingStatus: String
) {
  def desc: String = {
    s"bmUnitId: '$bmUnitId' ($bmUnitName), gc: $gc, interconnectorId: $interconnectorId"
  }
}

def toEntry(l: String): Entry = {
  def q(s: String): String = s.headOption match {
    case None => ""
    case Some('"') => s.drop(1).dropRight(1)
    case Some(_) => s
  }
  val p = l.split(',').map(x => q(x))
  Entry(p(0), p(1), p(2), p(3), p(4), p(5), p(6), p(7), p(8), p(9),
    p(10), p(11), p(12), p(13), p(14), p(15), p(16), p(17), p(18), p(19),
    p(20), p(21), p(22), p(23))
  /*l.split(',') match {
    case Array(a, b, c, d, e, f, g, h, i, j, k, l, m, n, _*) =>
      Entry(q(a), q(b), q(c), q(d), q(e), q(f), q(g), q(h), q(i), q(j), q(k), q(l), q(m), q(n))
  }*/
}

val entries = l.map(x => toEntry(x))
val ics = entries.filter(_.interconnectorId != "")
val byIcId = ics.groupBy(_.interconnectorId)
for ((k, v) <- byIcId) {
  println(s"$k : ${v.size}")
}

val gens = entries.filter(x => x.gc.toDouble > 0.0 && x.interconnectorId == "")
for (g <- gens) {
  println(g.desc)
}

/*
def g(e: Entry): String = if (e.tradingUnitName != "") e.tradingUnitName else e.partyName

val tu = entries.groupBy(x => g(x))
for (z <- tu.filter(_._2.size < 12)) {
  println(s"${z._1} : ${z._2.size}")
  for (zz <- z._2) {
    //println(s"  $zz")
  }
}
 */
