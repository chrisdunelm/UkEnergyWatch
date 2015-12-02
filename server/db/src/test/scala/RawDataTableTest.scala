package org.ukenergywatch.db

import org.scalatest.{FunSuite, Matchers}
import scala.concurrent.duration
import scala.concurrent._
import org.ukenergywatch.utils.JavaTimeExtensions._

class RawDataTableTest extends FunSuite with Matchers {

  object Components extends DbMemoryComponent
  import Components.db.driver.api._
  import Components.db._

  def v(start: Int, startValue: Double, length: Int = 1, deltaValue: Double = 0.0, name: String = "a"): RawData = {
    RawData(
      rawDataType = RawDataType.actualGeneration,
      name = name,
      fromTime = DbTime(start.minutesToInstant),
      toTime = DbTime((start + length).minutesToInstant),
      fromValue = startValue,
      toValue = startValue + deltaValue
    )
  }

  def run[E <: Effect](actions: DBIOAction[_, NoStream, E]*): Seq[RawData] = {
    val allActions = rawDatas.schema.create andThen
      DBIO.seq(actions: _*) andThen
      rawDatas.sortBy(_.fromTime).result
    val fResult = db.run(allActions.withPinnedSession)
    Await.result(fResult, duration.Duration(1, duration.SECONDS))
  }

  test("write-read") {
    val value = v(0, 1.0)
    val result = run(
      rawDatas += value
    )
    result.size shouldBe 1
    result(0).copy(id = 0) shouldBe value
  }

  test("Merge possible") {
    val result = run(
      rawDatas += v(0, 1.0),
      rawDatas.merge(v(1, 1.0))
    )
    result.size shouldBe 1
    result(0).id0 shouldBe v(0, 1.0, length = 2)
  }

  test("Merge impossible; value wrong") {
    val result = run(
      rawDatas += v(0, 1.0),
      rawDatas.merge(v(1, 2.0)) 
    )
    result.size shouldBe 2
    result(0).id0 shouldBe v(0, 1.0)
    result(1).id0 shouldBe v(1, 2.0)
  }

  test("Merge impossible; non-constant values") {
    val result = run(
      rawDatas += v(0, 1.0, deltaValue = 1.0),
      rawDatas.merge(v(1, 1.0, deltaValue = 1.0))
    )
    result.size shouldBe 2
    result(0).id0 shouldBe v(0, 1.0, deltaValue = 1.0)
    result(1).id0 shouldBe v(1, 1.0, deltaValue = 1.0)
  }

}
