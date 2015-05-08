package org.ukenergywatch.db

import org.scalatest._

import scala.concurrent.duration._
import scala.concurrent._

class TableTest extends FunSuite with Matchers {

  object Modules extends
      DbModuleMemory

  import Modules.{db, tables}
  import Modules.tables._
  import Modules.tables.driver.api._

  def m(minute: Int): Int = minute * 60
  def await[T](f: Future[T]): T = Await.result(f, 1.second)

  implicit class RichAggregate(v: Aggregate) {
    def id0 = v.copy(id = 0)
  }

  test("simple write+read 1") {
    val setup = DBIO.seq(
      (tables.aggregates.schema).create,
      tables.aggregates += Aggregate(Interval.hour, AggregateFunction.average, Location.genUnit, m(0), m(60))
    )
    val query = tables.aggregates.result
    val actions = setup andThen query
    val r = await(db.run(actions.withPinnedSession))
    r.size shouldBe 1
    r(0).id0 shouldBe Aggregate(Interval.hour, AggregateFunction.average, Location.genUnit, m(0), m(60))
  }

  test("simple write+read 2") {
    val setup = DBIO.seq(
      (tables.aggregates.schema).create,
      tables.aggregates += Aggregate(Interval.day, AggregateFunction.peak, Location.powerStation, m(60), m(120))
    )
    val query = tables.aggregates.result
    val actions = setup andThen query
    val r = await(db.run(actions.withPinnedSession))
    r.size shouldBe 1
    r(0).id0 shouldBe Aggregate(Interval.day, AggregateFunction.peak, Location.powerStation, m(60), m(120))
  }

}
