package org.ukenergywatch.data

import org.ukenergywatch.utils.LogComponent
import scala.concurrent.Future
import slick.dbio._
import org.ukenergywatch.db._
import org.ukenergywatch.utils.RangeOfExtensions._
import org.ukenergywatch.utils.{ RangeOf, AlignedRangeOf, SimpleRangeOfValue, SimpleRangeOf }
import org.ukenergywatch.utils.maths.Implicits._
import org.ukenergywatch.utils.JavaTimeExtensions._
import org.ukenergywatch.utils.units._
import java.time.Instant

// Might want to replace this later
import scala.concurrent.ExecutionContext.Implicits.global

trait DataComponent {
  this: DbComponent with LogComponent =>

  lazy val data = new Data

  class Data {
    import db.driver.api._

    private def calculateRawAggregate(
      aggregationType: AggregationType,
      name: Name,
      rawDatas: Seq[RawData],
      alignedRange: RangeOf[Instant]
    ): Aggregate = {
      val data: Seq[SimpleRangeOfValue[Instant, Power]] = rawDatas.map { rawData =>
        val rawFromTime = implicitly[Ordering[Instant]].max(rawData.fromTime.toInstant, alignedRange.from)
        val rawToTime = implicitly[Ordering[Instant]].min(rawData.toTime.toInstant, alignedRange.to)
        val fromPower: Power = Power.watts(rawData.interpolatedValue(rawFromTime))
        val toPower: Power = Power.watts(rawData.interpolatedValue(rawToTime))
        SimpleRangeOfValue(rawFromTime, rawToTime, fromPower, toPower)
      }
      val dataN = data.last
      def getAt(t: Instant): Power = {
        // This slightly convoluted code to get the correct value at the very end of the time
        //println(s"$t, $aggregationType, $name")
        data.filter(x =>
          (t >= x.from && t < x.to) || (t == x.to && t == dataN.to)
        ).map(_.interpolatedValue(t)).fold(Power.zero)(_ + _)
      }
      val meanPower: Power = data.map { data =>
        (data.value0 + data.value1) * 0.5 * (data.to - data.from)
      }.reduce(_ + _) / (alignedRange.to  - alignedRange.from)
      val minPower: Power = data.flatMap { d => Seq(getAt(d.from), getAt(d.to)) }.min
      val maxPower: Power = data.flatMap { d => Seq(getAt(d.from), getAt(d.to)) }.max
      val aggregations = Map(
        AggregationFunction.mean -> meanPower.watts,
        AggregationFunction.minimum -> minPower.watts,
        AggregationFunction.maximum -> maxPower.watts
      )
      val percentiles =
        (for (i <- 0 to 100) yield getAt(alignedRange.atFraction(i.toDouble / 100.0)))
          .sorted.updated(0, minPower).updated(100, maxPower)
          .zipWithIndex.map {
          case (p, i) => AggregationFunction.percentile(i) -> p.watts
       }.toMap
      Aggregate(
        AggregationInterval.hour,
        aggregationType,
        name.name,
        DbTime(alignedRange.from),
        DbTime(alignedRange.to),
        aggregations ++ percentiles
      ).autoSearchIndex
    }

    def hourlyAggregateFromRaw(
      rawDataType: RawDataType,
      aggregationType: AggregationType,
      aggregationFn: Seq[RawData] => Map[Name, Seq[RawData]],
      limit: Int = 1
    ): DBIO[Unit] = {
      val qRawProgress = db.rawProgresses
        .filter(_.rawDataType === rawDataType)
        .sortBy(_.fromTime)
      val qAggregateProgress = db.aggregateProgresses
        .filter(x => x.aggregationInterval === AggregationInterval.hour && x.aggregationType === aggregationType)
        .sortBy(_.fromTime)
      (qRawProgress.result zip qAggregateProgress.result).flatMap { case (rawProgress, aggregateProgress) =>
        val unaggregatedRanges = rawProgress - aggregateProgress
        val alignedRanges: Seq[RangeOf[Instant]] = AlignedRangeOf.hour(unaggregatedRanges).take(limit)
        val actions: Seq[DBIO[_]] = alignedRanges.map { alignedRange: RangeOf[Instant] =>
          val qRawData = db.rawDatas.search(alignedRange).filter(_.rawDataType === rawDataType)
          val insertAggregates: DBIO[_] = qRawData.result.flatMap { rawDatas: Seq[RawData] =>
            log.info(s"Data: hourlyAggregateFromRaw ${alignedRange.from} -> ${alignedRange.to} " +
              s"from ${rawDatas.size} raw data items, rawDataType:$rawDataType, aggregationType:$aggregationType")
            val rawDataGrouped: Map[Name, Seq[RawData]] = aggregationFn(rawDatas)
            val aggregates: Seq[Aggregate] = rawDataGrouped.toSeq.map { case (name, rawDatas) =>
              calculateRawAggregate(aggregationType, name, rawDatas, alignedRange)
            }
            db.aggregates ++= aggregates // Don't merge, very unlikely to be mergeable
          }
          val insertProgress: DBIO[_] = db.aggregateProgresses.merge(
            AggregateProgress(
              AggregationInterval.hour, aggregationType, DbTime(alignedRange.from), DbTime(alignedRange.to)
            )
          )
          (insertAggregates >> insertProgress).transactionally
        }
        DBIOAction.seq(actions: _*)
      }
    }

    // TODO: Move this elsewhere. All other functions here are not rawdata-type specific
    def actualGenerationHourAggregatesFromRaw(limit: Int = 1): DBIO[Unit] = {
      // Creates all hourly aggregations from actual-generation
      val generationUnit = hourlyAggregateFromRaw(
        RawDataType.Electric.actualGeneration,
        AggregationType.Electric.generationUnit,
        data => data.groupBy(x => BmuId(x.name))
      )
      val tradingUnit = hourlyAggregateFromRaw(
        RawDataType.Electric.actualGeneration,
        AggregationType.Electric.tradingUnit,
        data => data.groupBy { x =>
          StaticData.tradingUnitsByBmuId.get(BmuId(x.name))
            .map(_.name)
            .getOrElse(TradingUnitName.empty)
            .asInstanceOf[Name]
        } - TradingUnitName.empty
      )
      val uk = hourlyAggregateFromRaw(
        RawDataType.Electric.actualGeneration,
        AggregationType.Electric.regionalGeneration,
        data => Map(Region.uk -> data)
      )
      generationUnit >> tradingUnit >> uk
    }

    private def alignedRangeFn(interval: AggregationInterval): Seq[RangeOf[Instant]] => Seq[RangeOf[Instant]] = {
      interval match {
        case AggregationInterval.hour => AlignedRangeOf.hour _
        case AggregationInterval.day => AlignedRangeOf.day _
        case AggregationInterval.week => AlignedRangeOf.week _
        case AggregationInterval.month => AlignedRangeOf.month _
        case AggregationInterval.year => AlignedRangeOf.year _
        case _ => throw new Exception
      }
    }

    def calculateSubAggregates(
      aggregationType: AggregationType,
      sourceInterval: AggregationInterval,
      destinationInterval: AggregationInterval,
      limit: Int = 1
    ): DBIO[Unit] = {
      val qSource = db.aggregateProgresses
        .filter(x => x.aggregationInterval === sourceInterval && x.aggregationType === aggregationType)
        .sortBy(_.fromTime)
      val qDestination = db.aggregateProgresses
        .filter(x => x.aggregationInterval === destinationInterval && x.aggregationType === aggregationType)
      (qSource.result zip qDestination.result).flatMap { case (sources, destinations) =>
        val unaggregatedRanges = sources - destinations
        val alignedRanges: Seq[RangeOf[Instant]] =
          alignedRangeFn(destinationInterval)(unaggregatedRanges).take(limit)
        val actions: Seq[DBIO[_]] = alignedRanges.map { alignedRange: RangeOf[Instant] =>
          val alignedRangeFrom = DbTime(alignedRange.from)
          val alignedRangeTo = DbTime(alignedRange.to)
          val qSourceData = db.aggregates.search(alignedRange.from, alignedRange.to).filter { x =>
            x.aggregationInterval === sourceInterval && x.aggregationType === aggregationType
          }
          val insertAggregates: DBIO[_] = qSourceData.result.flatMap { sourceDatas: Seq[Aggregate] =>
            log.info(s"Data: calculateSubAggregates ${alignedRange.from} -> ${alignedRange.to} " +
              s"from ${sourceDatas.size} source items, aggregationType:$aggregationType, " +
              s"sourceInterval:$sourceInterval, destinationInterval:$destinationInterval")
            case class NameType(name: String, aggregationType: AggregationType)
            val sourceDataByNameType = sourceDatas.groupBy(x => NameType(x.name, x.aggregationType))
            val destAggs: Seq[Aggregate] = sourceDataByNameType.toSeq.map {
              case (nameType, sourceDatas: Seq[Aggregate]) =>
                // All the source aggregates will have the same duration, so mean can be calculated simply
                // (OK, not quite true for year aggregation, but never mind for now)
                def getFn(aggregationFunction: AggregationFunction): Seq[Power] = sourceDatas.map { d =>
                  Power.watts(d.value(aggregationFunction))
                }
                val meanPower: Power = getFn(AggregationFunction.mean)
                  .reduce(_ + _) * (1.0 / sourceDatas.size.toDouble)
                val minPower: Power = getFn(AggregationFunction.minimum).min
                val maxPower: Power = getFn(AggregationFunction.maximum).max
                val aggregations = Map(
                  AggregationFunction.mean -> meanPower.watts,
                  AggregationFunction.minimum -> minPower.watts,
                  AggregationFunction.maximum -> maxPower.watts
                )
                val sourcePercentiles: IndexedSeq[Power] = sourceDatas.flatMap { d =>
                  (0 to 100).map { i => Power.watts(d.value(AggregationFunction.percentile(i))) }
                }.sorted.toIndexedSeq
                val percentiles = (0 to 100).map { i =>
                  val idx = math.round((i.toDouble * (sourcePercentiles.length - 1).toDouble) / 100.0).toInt
                  AggregationFunction.percentile(i) -> sourcePercentiles(idx).watts
                }.toMap
                Aggregate(
                  destinationInterval,
                  nameType.aggregationType,
                  nameType.name,
                  alignedRangeFrom,
                  alignedRangeTo,
                  aggregations ++ percentiles
                ).autoSearchIndex
            }
            db.aggregates ++= destAggs // Merge instead? Probably not, very unlikely to match
          }
          val insertAggregateProgress = db.aggregateProgresses.merge(AggregateProgress(
            destinationInterval, aggregationType, alignedRangeFrom, alignedRangeTo
          ))
          (insertAggregates >> insertAggregateProgress).transactionally
        }
        DBIOAction.seq(actions: _*)
      }
    }

    def missingRawProgress(rawDataType: RawDataType, extremes: RangeOf[Instant]): DBIO[Seq[RangeOf[Instant]]] = {
      val qRawProgress = db.rawProgresses.filter(_.rawDataType === rawDataType).sortBy(_.fromTime)
      qRawProgress.result.map { rawProgress: Seq[RawProgress] => Seq(extremes) - rawProgress }
    }

    // TODO: Move all the following elsewhere, so nothing here is datatype-specific
    private def actualGenerationSubAggregates(
      sourceInterval: AggregationInterval, destinationInterval: AggregationInterval, limit: Int
    ): DBIO[Unit] = {
      calculateSubAggregates(AggregationType.Electric.generationUnit, sourceInterval, destinationInterval, limit) >>
        calculateSubAggregates(AggregationType.Electric.tradingUnit, sourceInterval, destinationInterval, limit) >>
        calculateSubAggregates(AggregationType.Electric.regionalGeneration, sourceInterval, destinationInterval, limit)
    }

    def actualGenerationSubAggregatesDay(limit: Int = 1): DBIO[Unit] = {
      actualGenerationSubAggregates(AggregationInterval.hour, AggregationInterval.day, limit)
    }

    def actualGenerationSubAggregatesWeek(limit: Int = 1): DBIO[Unit] = {
      actualGenerationSubAggregates(AggregationInterval.day, AggregationInterval.week, limit)
    }

    def actualGenerationSubAggregatesMonth(limit: Int = 1): DBIO[Unit] = {
      actualGenerationSubAggregates(AggregationInterval.day, AggregationInterval.month, limit)
    }

    def actualGenerationSubAggregatesYear(limit: Int = 1): DBIO[Unit] = {
      actualGenerationSubAggregates(AggregationInterval.month, AggregationInterval.year, limit)
    }

  }

}
