package org.ukenergywatch.data

case class Location(lng: Double, lat: Double)

case class BmuId(id: String) extends AnyVal

case class TradingUnit(
  name: String,
  bmuIds: Seq[BmuId],
  location: Location
)

// Data available via APIs:
// * BMU details (name, maximum power output, is-interconnector)

object StaticData {
  // Static data that is not available via any APIs

  val tradingUnits = Seq(
    TradingUnit(
      name = "Drax Power Station",
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
      ),
      Location(0, 0)
    ),

    TradingUnit(
      name = "London Array Offshore Windfarm",
      bmuIds = Seq(
        BmuId("T_LARYW-1"),
        BmuId("T_LARYW-2"),
        BmuId("T_LARYW-3"),
        BmuId("T_LARYW-4")
      ),
      Location(0, 0)
    )
  )

}
