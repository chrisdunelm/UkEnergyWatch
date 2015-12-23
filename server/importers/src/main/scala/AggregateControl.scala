package org.ukenergywatch.importers

import org.ukenergywatch.data.DataComponent
import org.ukenergywatch.db.DbComponent
import org.ukenergywatch.db.{ RawDataType, AggregationType, AggregationInterval }
import org.ukenergywatch.data.{ StaticData, BmuId, TradingUnitName, Name, Region, FuelType }
import java.time.Duration
import scala.concurrent.ExecutionContext

trait AggregateControlComponent {
  this: DbComponent with DataComponent =>

  lazy val aggregateControl = new AggregateControl

  class AggregateControl {
    import db.driver.api._

    private val intervals = Seq(
      AggregationInterval.hour -> AggregationInterval.day,
      AggregationInterval.day -> AggregationInterval.week,
      AggregationInterval.day -> AggregationInterval.month,
      AggregationInterval.month -> AggregationInterval.year
    )

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
      val subAggActions = for {
        aggregationType <- aggregationTypes
        (sourceInterval, destinationInterval) <- intervals
      } yield {
        data.calculateSubAggregates(aggregationType, sourceInterval, destinationInterval, limit)
      }
      val actions = generationUnit >> tradingUnit >> uk >> DBIO.seq(subAggActions: _*)
      db.executeAndWait(actions, timeout)
    }

    def fuelInst(limit: Int, timeout: Duration)(implicit ec: ExecutionContext): Unit = {
      val fuelType = data.hourlyAggregateFromRaw(
        RawDataType.Electric.generationByFuelType,
        AggregationType.Electric.fuelType,
        data => data.groupBy(x => FuelType(x.name)),
        limit
      )
      val uk = data.hourlyAggregateFromRaw(
        RawDataType.Electric.generationByFuelType,
        AggregationType.Electric.regionalFuelType,
        data => Map(Region.uk -> data),
        limit
      )
      val aggregationTypes = Seq(
        AggregationType.Electric.fuelType,
        AggregationType.Electric.regionalFuelType
      )
      val subAggActions = for {
        aggregationType <- aggregationTypes
        (sourceInterval, destinationInterval) <- intervals
      } yield {
        data.calculateSubAggregates(aggregationType, sourceInterval, destinationInterval, limit)
      }
      val actions = fuelType >> uk >> DBIO.seq(subAggActions: _*)
      db.executeAndWait(actions, timeout)
    }

    def frequency(limit: Int, timeout: Duration)(implicit ec: ExecutionContext): Unit = {
      val hour = data.hourlyAggregateFromRaw(
        RawDataType.Electric.frequency,
        AggregationType.Electric.frequency,
        data => Map(Region.uk -> data),
        limit
      )
      val subAggActions = for {
        (sourceInterval, destinationInterval) <- intervals
      } yield {
        data.calculateSubAggregates(AggregationType.Electric.frequency, sourceInterval, destinationInterval, limit)
      }
      val actions = hour >> DBIO.seq(subAggActions: _*)
      db.executeAndWait(actions, timeout)
    }

  }

}
