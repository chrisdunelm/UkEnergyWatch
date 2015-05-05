package org.ukenergywatch.db

import org.scalatest._

import scala.concurrent.duration._
import scala.concurrent._

class TableTest extends FunSuite with Matchers {

  object Modules extends
      DbModuleMemory

  import Modules.{db, tables}
  import Modules.tables.Aggregate
  import Modules.tables.driver.api._

  def m(minute: Int): Int = minute * 60
  def await[T](f: Future[T]): T = Await.result(f, 1.second)

  test("simple write+read") {
    val setup = DBIO.seq(
      (tables.aggregates.schema).create,
      tables.aggregates += Aggregate("g=DRAX1;d=1800", m(0), m(30), 100.0, 0)
    )
    val query = tables.aggregates.result
    val actions = setup andThen query
    val r = await(db.run(actions.withPinnedSession))
    r.size shouldBe 1
    r(0) shouldBe Aggregate("g=DRAX1;d=1800", m(0), m(30), 100.0, 0)
  }

  test("simple write+read 2") {
    val setup = DBIO.seq(
      (tables.aggregates.schema).create,
      tables.aggregates += Aggregate("g=DRAX1;d=1800", m(0), m(30), 100.0, 0)
    )
    val query = tables.aggregates.result
    val actions = setup andThen query
    val r = await(db.run(actions.withPinnedSession))
    r.size shouldBe 1
    r(0) shouldBe Aggregate("g=DRAX1;d=1800", m(0), m(30), 100.0, 0)
  }

}
