package org.ukenergywatch.db

import org.scalatest.{FunSuite, Matchers}
import org.joda.time.DateTime
import scala.concurrent.duration._
import scala.concurrent._

class RawDataTableTest extends FunSuite with Matchers {

  def await[T](f: Future[T]): T = Await.result(f, 1.second)

  test("write-read") {
    object Components extends DbMemoryComponent
    import Components.db.driver.api._
    import Components.db._

    val value = RawData(
      AggregationType.TradingUnit,
      "name",
      DbTime(new DateTime(2015, 1, 1, 0, 0)),
      DbTime(new DateTime(2015, 1, 1, 0, 1)),
      0.0,
      1.0
    )
    val setup = DBIO.seq(
      (rawDatas.schema).create,
      rawDatas += value
    )
    val query = rawDatas.result
    val r = await(db.run((setup andThen query).withPinnedSession))
    r.size shouldBe 1
    r(0).copy(id = 0) shouldBe value

  }

}
