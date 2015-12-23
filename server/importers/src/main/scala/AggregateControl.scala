package org.ukenergywatch.importers

import org.ukenergywatch.data.DataComponent
import org.ukenergywatch.db.DbComponent
import org.ukenergywatch.db.{ RawDataType, AggregationType, AggregationInterval }
import org.ukenergywatch.data.{ StaticData, BmuId, TradingUnitName, Name, Region }
import java.time.Duration
import scala.concurrent.ExecutionContext

trait AggregateControlComponent {
  this: DbComponent with DataComponent =>

  lazy val aggregateControl = new AggregateControl

  class AggregateControl {
    import db.driver.api._

    def actualGeneration(limit: Int, timeout: Duration)(implicit ec: ExecutionContext): Unit = {
      val generationUnit = data.hourlyAggregateFromRaw(
        RawDataType.Electric.actualGeneration,
        AggregationType.Electric.generationUnit,
        data => data.groupBy(x => BmuId(x.name)),
        limit
      )
      val tradingUnit = data.hourlyAggregateFromRaw(
        RawDataType.Electric.actualGeneration,
        AggregationType.Electric.tradingUnit,
        data => data.groupBy { x =>
          StaticData.tradingUnitsByBmuId.get(BmuId(x.name))
            .map(_.name)
            .getOrElse(TradingUnitName.empty)
            .asInstanceOf[Name]
        } - TradingUnitName.empty,
        limit
      )
      val uk = data.hourlyAggregateFromRaw(
        RawDataType.Electric.actualGeneration,
        AggregationType.Electric.regionalGeneration,
        data => Map(Region.uk -> data),
        limit
      )
      val aggregationTypes = Seq(
        AggregationType.Electric.generationUnit,
        AggregationType.Electric.tradingUnit,
        AggregationType.Electric.regionalGeneration
      )
      val intervals = Seq(
        AggregationInterval.hour -> AggregationInterval.day,
        AggregationInterval.day -> AggregationInterval.week,
        AggregationInterval.day -> AggregationInterval.month,
        AggregationInterval.month -> AggregationInterval.year
      )
      val subAggActions = for {
        aggregationType <- aggregationTypes
        (sourceInterval, destinationInterval) <- intervals
      } yield {
        data.calculateSubAggregates(aggregationType, sourceInterval, destinationInterval, limit)
      }
      val actions = generationUnit >> tradingUnit >> uk >> DBIO.seq(subAggActions: _*)
      db.executeAndWait(actions, timeout)
    }

  }

}
