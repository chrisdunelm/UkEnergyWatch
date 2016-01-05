package org.ukenergywatch.db

import org.scalatest.{FunSuite, Matchers}
import scala.concurrent._
import scala.concurrent.duration
import org.ukenergywatch.utils.JavaTimeExtensions._

import scala.language.existentials

class RawProgressTableTest extends FunSuite with Matchers {

  object Components extends DbMemoryComponent
  import Components.db.driver.api._
  import Components.db._

  def p(start: Int, length: Int = 1,
    rawDataType: RawDataType = RawDataType.Electric.actualGeneration
  ): RawProgress = RawProgress(
    rawDataType,
    DbTime(start.minutesToInstant),
    DbTime((start + length).minutesToInstant)
  )

  def run[E <: Effect](actions: DBIOAction[_, NoStream, E]*): Seq[RawProgress] = {
    val all = rawProgresses.schema.create andThen
      DBIO.seq(actions: _*) andThen
      rawProgresses.sortBy(_.fromTime).result
    val fResult = db.run(all.withPinnedSession)
    Await.result(fResult, duration.Duration(1, duration.SECONDS))
  }

  test("Initial item") {
    val result = run(rawProgresses.merge(p(0)))

    result.size shouldBe 1
    result(0).id0 shouldBe p(0)
  }

  test("Mergeable item inserted after") {
    val result = run(
      rawProgresses += p(0),
      rawProgresses.merge(p(1))
    )

    result.size shouldBe 1
    result(0).id0 shouldBe p(0, 2)
  }

  test("Mergeable item inserted after, but with a gap") {
    val result = run(
      rawProgresses += p(0),
      rawProgresses.merge(p(2))
    )

    result.size shouldBe 2
    result(0).id0 shouldBe p(0)
    result(1).id0 shouldBe p(2)
  }

  test("Non-mergeable item inserter after") {
    val result = run(
      rawProgresses += p(0),
      rawProgresses.merge(p(1, rawDataType = RawDataType.Electric.predictedGeneration))
    )
    result.size shouldBe 2
    result(0).id0 shouldBe p(0)
    result(1).id0 shouldBe p(1, rawDataType = RawDataType.Electric.predictedGeneration)
  }

  test("Mergeable item inserted before") {
    val result = run(
      rawProgresses += p(1),
      rawProgresses.merge(p(0))
    )

    result.size shouldBe 1
    result(0).id0 shouldBe p(0, 2)
  }

  test("Mergeable item inserted before, but with a gap") {
    val result = run(
      rawProgresses += p(2),
      rawProgresses.merge(p(0))
    )

    result.size shouldBe 2
    result(0).id0 shouldBe p(0)
    result(1).id0 shouldBe p(2)
  }

  test("Non-mergeable item inserter before") {
    val result = run(
      rawProgresses += p(1),
      rawProgresses.merge(p(0, rawDataType = RawDataType.Electric.predictedGeneration))
    )
    result.size shouldBe 2
    result(0).id0 shouldBe p(0, rawDataType = RawDataType.Electric.predictedGeneration)
    result(1).id0 shouldBe p(1)
  }

  test("Mergeable item inserted between") {
    val result = run(
      rawProgresses += p(2),
      rawProgresses += p(0),
      rawProgresses.merge(p(1))
    )
    result.size shouldBe 1
    result(0).id0 shouldBe p(0, 3)
  }

  test("Non-mergeable item inserted between") {
    val result = run(
      rawProgresses += p(2),
      rawProgresses += p(0),
      rawProgresses.merge(p(1, rawDataType = RawDataType.Electric.predictedGeneration))
    )
    result.size shouldBe 3
    result(0).id0 shouldBe p(0)
    result(1).id0 shouldBe p(1, rawDataType = RawDataType.Electric.predictedGeneration)
    result(2).id0 shouldBe p(2)
  }

}
