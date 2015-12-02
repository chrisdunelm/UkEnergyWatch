package org.ukenergywatch.data

import org.scalatest._

import org.ukenergywatch.db._
import org.ukenergywatch.data._
import org.ukenergywatch.utils.JavaTimeExtensions._

import scala.concurrent._
import scala.concurrent.duration
import slick.dbio._

class AggregatesTest extends FunSuite with Matchers {

  trait Components
      extends DbMemoryComponent
      with DataComponent {

    def createTables(): DBIO[_] = {
      import db.driver.api._
      val actions = db.rawDatas.schema.create andThen
        db.rawProgresses.schema.create andThen
        db.aggregateProgresses.schema.create andThen
        db.aggregates.schema.create
      actions
    }

  }

  def m(minute: Int): DbTime = DbTime(minute.minutesToInstant)

  test("mean-only single simple aggregation") {
    object Comps extends Components
    import Comps.db.driver.api._

    val insertRawProgress = Comps.db.rawProgresses ++= Seq(
      RawProgress(RawDataType.actualGeneration, m(0), m(60))
    )
    val insertRawData = Comps.db.rawDatas ++= Seq(
      RawData(RawDataType.actualGeneration, "a", m(0), m(60), 1.0, 1.0),
      RawData(RawDataType.actualGeneration, "b", m(0), m(30), 1.0, 3.0),
      RawData(RawDataType.actualGeneration, "b", m(30), m(60), 3.0, 5.0)
    )

    val actions = Comps.createTables() >>
      insertRawProgress >> insertRawData >>
      Comps.data.createAggregates() >>
      Comps.db.aggregates.result
    val fAggs = Comps.db.db.run(actions.withPinnedSession)
    val aggs: Seq[Aggregate] = Await.result(fAggs, duration.Duration(1, duration.SECONDS))
    aggs.find(_.name == "a").get.value(AggregationFunction.mean) shouldBe 1.0 +- 1e-10
    aggs.find(_.name == "b").get.value(AggregationFunction.mean) shouldBe 3.0 +- 1e-10
  }

}
