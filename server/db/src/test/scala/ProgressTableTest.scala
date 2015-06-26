package org.ukenergywatch.db

import org.scalatest.{FunSuite, Matchers}
import org.joda.time.DateTime
import scala.concurrent._
import scala.concurrent.duration._

import scala.language.existentials

class ProgressTableTest extends FunSuite with Matchers {

  object Components extends DbMemoryComponent
  import Components.db.driver.api._
  import Components.db._

  def p(start: Int, length: Int = 1, rawDataType: RawDataType = RawDataType.ActualGeneration): Progress = Progress(
    rawDataType,
    DbTime(new DateTime(2015, 1, 1, 0, start)),
    DbTime(new DateTime(2015, 1, 1, 0, start + length))
  )

  def run[E <: Effect](actions: DBIOAction[_, NoStream, E]*): Seq[Progress] = {
    val all = progresses.schema.create andThen
      DBIO.seq(actions: _*) andThen
      progresses.sortBy(_.fromTime).result
    val fResult = db.run(all.withPinnedSession)
    Await.result(fResult, 1.second)
  }

  test("Initial item") {
    val result = run(progresses.merge(p(0)))

    result.size shouldBe 1
    result(0).id0 shouldBe p(0)
  }

  test("Mergeable item inserted after") {
    val result = run(
      progresses += p(0),
      progresses.merge(p(1))
    )

    result.size shouldBe 1
    result(0).id0 shouldBe p(0, 2)
  }

  test("Mergeable item inserted after, but with a gap") {
    val result = run(
      progresses += p(0),
      progresses.merge(p(2))
    )

    result.size shouldBe 2
    result(0).id0 shouldBe p(0)
    result(1).id0 shouldBe p(2)
  }

  test("Non-mergeable item inserter after") {
    val result = run(
      progresses += p(0),
      progresses.merge(p(1, rawDataType = RawDataType.PredictedGeneration))
    )
    result.size shouldBe 2
    result(0).id0 shouldBe p(0)
    result(1).id0 shouldBe p(1, rawDataType = RawDataType.PredictedGeneration)
  }

  test("Mergeable item inserted before") {
    val result = run(
      progresses += p(1),
      progresses.merge(p(0))
    )

    result.size shouldBe 1
    result(0).id0 shouldBe p(0, 2)
  }

  test("Mergeable item inserted before, but with a gap") {
    val result = run(
      progresses += p(2),
      progresses.merge(p(0))
    )

    result.size shouldBe 2
    result(0).id0 shouldBe p(0)
    result(1).id0 shouldBe p(2)
  }

  test("Non-mergeable item inserter before") {
    val result = run(
      progresses += p(1),
      progresses.merge(p(0, rawDataType = RawDataType.PredictedGeneration))
    )
    result.size shouldBe 2
    result(0).id0 shouldBe p(0, rawDataType = RawDataType.PredictedGeneration)
    result(1).id0 shouldBe p(1)
  }

  test("Mergeable item inserted between") {
    val result = run(
      progresses += p(2),
      progresses += p(0),
      progresses.merge(p(1))
    )
    result.size shouldBe 1
    result(0).id0 shouldBe p(0, 3)
  }

  test("Non-mergeable item inserted between") {
    val result = run(
      progresses += p(2),
      progresses += p(0),
      progresses.merge(p(1, rawDataType = RawDataType.PredictedGeneration))
    )
    result.size shouldBe 3
    result(0).id0 shouldBe p(0)
    result(1).id0 shouldBe p(1, rawDataType = RawDataType.PredictedGeneration)
    result(2).id0 shouldBe p(2)
  }

}
