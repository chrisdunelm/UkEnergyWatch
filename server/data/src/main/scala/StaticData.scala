package org.ukenergywatch.data

case class Location(lng: Double, lat: Double)

trait Name extends Any {
  def name: String
}

case class BmuId(name: String) extends AnyVal with Name

case class TradingUnitName(name: String) extends AnyVal with Name
object TradingUnitName {
  val empty = TradingUnitName("")
}

case class TradingUnit(
  name: TradingUnitName,
  location: Location,
  bmuIds: Seq[BmuId]
)

// TODO: This should probably be defined somewhere else
case class Region(name: String) extends AnyVal with Name
object Region {
  val uk = Region("uk")
}

// Data available via APIs:
// * BMU details (name, maximum power output, is-interconnector)

object StaticData {
  // Static data.
  // Actually, this data can and does change.
  // Later get this from an API.

  object TradingUnits {
    val drax = TradingUnitName("Drax Power Station")
    val londonArray = TradingUnitName("London Array Offshore Windfarm")
  }

  val tradingUnits: Seq[TradingUnit] = Seq(
    TradingUnit(
      name = TradingUnits.drax,
      location = Location(0, 0),
      bmuIds = Seq(
        BmuId("T_DRAXX-1"),
        BmuId("T_DRAXX-2"),
        BmuId("T_DRAXX-3"),
        BmuId("T_DRAXX-4"),
        BmuId("T_DRAXX-5"),
        BmuId("T_DRAXX-6"),
        BmuId("T_DRAXX-9G"),
        BmuId("T_DRAXX-10G"),
        BmuId("T_DRAXX-12G")
      )
    ),

    TradingUnit(
      name = TradingUnits.londonArray,
      location = Location(0, 0),
      bmuIds = Seq(
        BmuId("T_LARYW-1"),
        BmuId("T_LARYW-2"),
        BmuId("T_LARYW-3"),
        BmuId("T_LARYW-4")
      )
    )
  )

  val tradingUnitsByTradingUnitName: Map[TradingUnitName, TradingUnit] =
    tradingUnits.map(x => (x.name, x)).toMap
  val tradingUnitsByBmuId: Map[BmuId, TradingUnit] =
    tradingUnits.flatMap(x => x.bmuIds.map(_ -> x)).toMap

}
