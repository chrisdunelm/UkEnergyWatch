
import scala.io.Source

def uq(s: String): String = s.headOption match {
  case None => ""
  case Some('"') => s.drop(1).dropRight(1)
  case Some(_) => s
}

case class BmUnit(
  bmUnitId: String,
  bmUnitName: String,
  partyName: String,
  partyId: String,
  tradingUnitName: String,
  gc: String,
  dc: String
)
object BmUnit {
  def apply(s: String): BmUnit = {
    val p = s.split(',').map(s => uq(s))
    BmUnit(p(0), p(1), p(2), p(3), p(8), p(12), p(13))
  }
}

case class B1420(
  resourceType: String,
  bmUnitId: String,
  voltageLimit: String,
  nominalPower: String
)
object B1420 {
  def apply(s: String): B1420 = {
    val p = s.split(',').map(s => uq(s))
    B1420(p(4), p(6), p(8), p(9))
  }
}

val bmUnits = Source.fromFile("./reg_bm_units.csv").getLines.drop(3).map(x => BmUnit(x))

val b1420Files = Seq(
  "B1420-2015.csv",
  "B1420-2014.csv",
  "B1420-2013.csv"
)

val b1420s = b1420Files.flatMap(x => Source.fromFile(x).getLines)
  .filterNot(_.startsWith("*")).filterNot(_ == "<EOF>")
  .map(x => B1420(x))

val b1420ByBmUnitId = b1420s.map(x => (x.bmUnitId, x)).toMap

var count = 0
for (bmu <- bmUnits) {
  val b1420 = b1420ByBmUnitId.get(bmu.bmUnitId)
  val desc = b1420 match {
    case None => None //"<unknown>"
    case Some(b1420) => Some(s"'${b1420.resourceType}'")
  }
  desc.foreach { desc =>
    println(s"'${bmu.bmUnitId}' : $desc")
    count += 1
  }
}

println(s"Count = $count")
