package org.ukenergywatch.data

import scala.concurrent.Future
import slick.dbio._
import org.ukenergywatch.db._
import org.ukenergywatch.utils.RangeOfExtensions._
import org.ukenergywatch.utils.{ RangeOf, AlignedRangeOf, SimpleRangeOfValue }
import org.ukenergywatch.utils.maths.Implicits._
import org.ukenergywatch.utils.JavaTimeExtensions._
import org.ukenergywatch.utils.units._
import java.time.Instant

// Might want to replace this later
import scala.concurrent.ExecutionContext.Implicits.global

trait DataComponent {
  this: DbComponent =>

  lazy val data = new Data

  class Data {
    import db.driver.api._

    def database = db.db

    def writeRaw(raw: Seq[RawData]): Unit = {
      database.run(db.rawDatas ++= raw)
    }

    def calculateRawAggregate(
      aggregationType: AggregationType, name: Name,
      rawDatas: Seq[RawData], alignedRange: RangeOf[Instant]
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
        data.filter(x =>
          (t >= x.from && t < x.to) || (t == x.to && t == dataN.to)
        ).map(_.interpolatedValue(t)).reduce(_ + _)
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
      )
    }

    def createAggregatesFromRaw(): DBIO[Unit] = {
      // Creates all hourly aggregations from actual-generation
      // TODO: Fuel-type aggregation
      val qRawProgresses = db.rawProgresses
        .filter(_.rawDataType === RawDataType.actualGeneration)
        .sortBy(_.fromTime)
      val qAggregateProgresses = db.aggregateProgresses
        .filter(x => x.aggregationInterval === AggregationInterval.hour &&
          x.aggregationType === AggregationType.generationUnit)
        .sortBy(_.fromTime)
      (qRawProgresses.result zip qAggregateProgresses.result).flatMap {
        case (rawProgress: Seq[RawProgress], aggProgress: Seq[AggregateProgress]) =>
          val unaggregatedRanges = rawProgress - aggProgress
          val alignedRanges = AlignedRangeOf.hour(unaggregatedRanges)
          // For each hour range, load all the raw data for that hour
          val actions: Seq[DBIO[_]] = alignedRanges.map { alignedRange: RangeOf[Instant] =>
            val alignedRangeTo = DbTime(alignedRange.to)
            val alignedRangeFrom = DbTime(alignedRange.from)
            val qRawData = db.rawDatas.filter { x =>
              x.rawDataType === RawDataType.actualGeneration &&
              x.fromTime < alignedRangeTo && x.toTime > alignedRangeFrom
            }
            val insertAggregates: DBIO[_] = qRawData.result.flatMap { rawDatas: Seq[RawData] =>
              val rawDataByBmuId: Map[BmuId, Seq[RawData]] = rawDatas.groupBy(x => BmuId(x.name))
              val rawDataByTradingUnit: Map[TradingUnitName, Seq[RawData]] = rawDatas.groupBy { x =>
                StaticData.tradingUnitsByBmuId.get(BmuId(x.name)).map(_.name).getOrElse(TradingUnitName.empty)
              } - TradingUnitName.empty
              val bmuAggregates = rawDataByBmuId.toSeq.map { case (bmuId: BmuId, rawDatas: Seq[RawData]) =>
                calculateRawAggregate(AggregationType.generationUnit, bmuId, rawDatas, alignedRange)
              }
              val tradingUnitAggregates = rawDataByTradingUnit.toSeq.map { case (tradingUnitName, rawDatas) =>
                calculateRawAggregate(AggregationType.tradingUnit, tradingUnitName, rawDatas, alignedRange)
              }
              val ukAggregate = calculateRawAggregate(AggregationType.region, Region.uk, rawDatas, alignedRange) 
              val allAggregates = bmuAggregates ++ tradingUnitAggregates :+ ukAggregate
              db.aggregates ++= allAggregates
            }
            val insertAggregateProgress = db.aggregateProgresses += AggregateProgress(
              AggregationInterval.hour, AggregationType.generationUnit, alignedRangeFrom, alignedRangeTo
            )
            insertAggregates >> insertAggregateProgress
          }
          DBIOAction.seq(actions: _*)
      }
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

    def createSubAggregates(
      sourceInterval: AggregationInterval,
      destinationInterval: AggregationInterval
    ): DBIO[Unit] = {
      val qSource = db.aggregateProgresses
        .filter(x => x.aggregationInterval === sourceInterval &&
          x.aggregationType === AggregationType.generationUnit)
        .sortBy(_.fromTime)
      val qDestination = db.aggregateProgresses
        .filter(x => x.aggregationInterval === destinationInterval &&
          x.aggregationType === AggregationType.generationUnit)
      (qSource.result zip qDestination.result).flatMap { case (sources, destinations) =>
        val unaggregatedRanges = sources - destinations
        val alignedRanges = alignedRangeFn(destinationInterval)(unaggregatedRanges)
        val actions: Seq[DBIO[_]] = alignedRanges.map { alignedRange: RangeOf[Instant] =>
          val alignedRangeFrom = DbTime(alignedRange.from)
          val alignedRangeTo = DbTime(alignedRange.to)
          val qSourceData = db.aggregates.filter { x =>
            x.aggregationInterval === sourceInterval &&
            x.fromTime < alignedRangeTo && x.toTime > alignedRangeFrom
          }
          val insertAggregates: DBIO[_] = qSourceData.result.flatMap { sourceDatas: Seq[Aggregate] =>
            case class NameType(name: String, aggregationType: AggregationType)
            val sourceDataByNameType = sourceDatas.groupBy(x => NameType(x.name, x.aggregationType))
            val destAggs: Seq[Aggregate] = sourceDataByNameType.toSeq.map {
              case (nameType, sourceDatas: Seq[Aggregate]) =>
                // All the source aggregates will have the same duration, so mean can be calculated simply
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
                )
            }
            db.aggregates ++= destAggs
          }
          val insertAggregateProgress = db.aggregateProgresses += AggregateProgress(
            destinationInterval, AggregationType.generationUnit, alignedRangeFrom, alignedRangeTo
          )
          insertAggregates >> insertAggregateProgress
        }
        DBIOAction.seq(actions: _*)
      }
    }

  }

}
