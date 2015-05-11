
import scala.io.Source

val files = Seq(
  "B1420-2015.csv"
)

val lines = files.flatMap(x => Source.fromFile(x).getLines)
  .filterNot(_.startsWith("*")).filterNot(_ == "<EOF>")

case class B1420(
  documentType: String,
  businessType: String,
  processType: String,
  resourceType: String,
  year: String,
  bmuId: String,
  resourceEicCode: String,
  voltageLimit: String,
  nominal: String, // Power output?
  ngcBmuId: String,
  resourceName: String,
  activeFlag: String,
  implementationDate: String
)

def toB1420(l: String): B1420 = {
  def q(s: String): String = s.headOption match {
    case None => ""
    case Some('"') => s.drop(1).dropRight(1)
    case Some(_) => s
  }
  val p = l.split(',').map(x => q(x))
  println(p)
  B1420(p(0), p(1), p(2), p(4), p(5), p(6), p(7), p(8), p(9), p(10), p(11), p(12), p(13))
}

val b1420s = lines.map(x => toB1420(x))

for (z <- b1420s) {
  println(z)
}
