package org.ukenergywatch.db

import org.scalatest._

import scala.concurrent.duration
import scala.concurrent._

import java.time.Instant
import org.ukenergywatch.utils.JavaTimeExtensions._

class AggregatesTest extends FunSuite with Matchers {

  def await[T](f: Future[T]): T = Await.result(f, duration.Duration(1, duration.SECONDS))

  test("write-read") {
    object Components extends DbMemoryComponent
    import Components.db.driver.api._
    import Components.db._

    val value = Aggregate(
      AggregationInterval.hour,
      AggregationType.generationUnit,
      "genunit1",
      DbTime(0.secondsToInstant),
      DbTime(1.secondsToInstant),
      Map(AggregationFunction.average -> 11.0)
    )

    val setup = DBIO.seq(
      (aggregates.schema).create,
      aggregates += value
    )

    val query = aggregates.result
    val r = await(db.run((setup andThen query).withPinnedSession))
    r.size shouldBe 1
    r(0).copy(id = 0) shouldBe value

    // Check pattern-matching works
    r(0).aggregationInterval should matchPattern {
      case AggregationInterval.hour =>
    }
    r(0).aggregationInterval should not matchPattern {
      case AggregationInterval.day =>
    }
  }

}
