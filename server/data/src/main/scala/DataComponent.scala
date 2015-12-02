package org.ukenergywatch.data

import scala.concurrent.Future
import slick.dbio._
import org.ukenergywatch.db._
import org.ukenergywatch.utils.RangeOfExtensions._
import org.ukenergywatch.utils.{ RangeOf, AlignedRangeOf }
import org.ukenergywatch.utils.maths.Implicits._
import org.ukenergywatch.utils.JavaTimeExtensions._
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

    def createAggregates(): DBIO[_] = {
      // Just do actual-generation, generation-unit hourly
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
              val nameMean = rawDatas.groupBy(_.name).toSeq.map { case (name: String, rawDatas: Seq[RawData]) =>
                // TODO: All aggregation functions
                val totalEnergy: Double = rawDatas.map { rawData =>
                  val rawFromTime = implicitly[Ordering[Instant]].max(rawData.fromTime.toInstant, alignedRange.from)
                  val rawToTime = implicitly[Ordering[Instant]].min(rawData.toTime.toInstant, alignedRange.to)
                  val fromPower: Double = rawData.interpolatedValue(rawFromTime)
                  val toPower: Double = rawData.interpolatedValue(rawToTime)
                  val energy = (fromPower + toPower) * 0.5 * (rawToTime - rawFromTime).secondsDouble
                  energy
                }.sum
                val power = totalEnergy / 3600.0
                name -> power
              }
              val aggregates: Seq[Aggregate] = nameMean.map { case (name: String, meanPower: Double) =>
                Aggregate(
                  AggregationInterval.hour,
                  AggregationType.generationUnit,
                  name,
                  alignedRangeFrom,
                  alignedRangeTo,
                  Map(AggregationFunction.mean -> meanPower)
                )
              }
              db.aggregates ++= aggregates
            }
            insertAggregates
          }
          DBIOAction.seq(actions: _*)
      }
    }

  }

}
