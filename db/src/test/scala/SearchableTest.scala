package org.ukenergywatch.db

import org.scalatest._
import org.ukenergywatch.utils.SimpleRangeOf
import org.ukenergywatch.utils.JavaTimeExtensions._
import java.time.Instant

class SearchableTest extends FunSuite with Matchers {

  def hourRange(fromHour: Int, toHour: Int): SimpleRangeOf[Instant] =
    SimpleRangeOf(fromHour.hoursToInstant, toHour.hoursToInstant)

  def o(level: Int): Int = SearchableValue.levelOffset(level)

  test("searchIndex") {
    SearchableValue.searchIndex(hourRange(0, 1)) shouldBe SearchableValue.levelOffset(0) + 0
    SearchableValue.searchIndex(hourRange(0, 2)) shouldBe SearchableValue.levelOffset(0) + 0
    SearchableValue.searchIndex(hourRange(1, 2)) shouldBe SearchableValue.levelOffset(0) + 1

    SearchableValue.searchIndex(hourRange(0, 16)) shouldBe SearchableValue.levelOffset(1) + 0
    SearchableValue.searchIndex(hourRange(8, 16)) shouldBe SearchableValue.levelOffset(1) + 1
    SearchableValue.searchIndex(hourRange(16, 19)) shouldBe SearchableValue.levelOffset(1) + 2

    SearchableValue.searchIndex(hourRange(0, 128)) shouldBe SearchableValue.levelOffset(2) + 0
    SearchableValue.searchIndex(hourRange(64, 128)) shouldBe SearchableValue.levelOffset(2) + 1
    SearchableValue.searchIndex(hourRange(128, 256)) shouldBe SearchableValue.levelOffset(2) + 2

    SearchableValue.searchIndex(hourRange(128, 257)) shouldBe SearchableValue.levelOffset(3) + 0
  }

  test("searchRange") {
    SearchableValue.searchRanges(hourRange(0, 1)).toSet shouldBe Set(
      SimpleRangeOf(o(0) + 0, o(0) + 0),
      SimpleRangeOf(o(1) + 0, o(1) + 0),
      SimpleRangeOf(o(2) + 0, o(2) + 0),
      SimpleRangeOf(o(3) + 0, o(3) + 0),
      SimpleRangeOf(o(4) + 0, o(4) + 0),
      SimpleRangeOf(o(5) + 0, o(5) + 0),
      SimpleRangeOf(o(6) + 0, o(6) + 0)
    )

    SearchableValue.searchRanges(hourRange(1000, 2000)).toSet shouldBe Set(
      SimpleRangeOf(o(0) + 999, o(0) + 1999), // half-duration = 1 hour
      SimpleRangeOf(o(1) + 124, o(1) + 249), // half-duration = 8 hours
      SimpleRangeOf(o(2) + 14, o(2) + 31), // half-duration = 64 hours
      SimpleRangeOf(o(3) + 0, o(3) + 3), // half-duration = 512 hours
      SimpleRangeOf(o(4) + 0, o(4) + 0), // half-duration = 4096 hours
      SimpleRangeOf(o(5) + 0, o(5) + 0),
      SimpleRangeOf(o(6) + 0, o(6) + 0)
    )
  }

  import scala.concurrent._

  object Components extends DbMemoryComponent
  import Components.db.driver.api._
  import Components.db._

  def v(start: Int, startValue: Double, length: Int = 1, deltaValue: Double = 0.0, name: String = "a"): RawData = {
    val startInstant = start.minutesToInstant
    val endInstant = (start + length).minutesToInstant
    RawData(
      rawDataType = RawDataType.Electric.actualGeneration,
      name = name,
      fromTime = DbTime(startInstant),
      toTime = DbTime(endInstant),
      fromValue = startValue,
      toValue = startValue + deltaValue
    )
  }

  def run[E <: Effect](actions: DBIOAction[_, NoStream, E]*): Seq[RawData] = {
    val allActions = rawDatas.schema.create andThen
      DBIO.seq(actions: _*) andThen
      rawDatas.sortBy(_.fromTime).result
    val fResult = db.run(allActions.withPinnedSession)
    Await.result(fResult, 1.second.toConcurrent)
  }

  test("SearchMerge single insert") {
    val value = v(0, 0.0)
    val result = run(
      rawDatas merge value
    )
    result.head.searchIndex shouldBe SearchableValue.searchIndex(value)
  }

  test("SearchMerge long merge") {
    val value0 = v(0, 0.0, 1)
    val value1 = v(1, 0.0, 999) // Total length = 1000 minutes = 16 hours, 20 minutes
    val result = run(
      rawDatas merge value0,
      rawDatas merge value1
    )
    result.head.searchIndex shouldBe SearchableValue.searchIndex(v(0, 0.0, 1000))
  }

  def runSearch(actions: DBIO[_]*)(from: Instant, to: Instant): Seq[RawData] = {
    val fResult = db.run(
      (rawDatas.schema.create >>
        DBIO.seq(actions: _*) >>
        rawDatas.search(from, to).result
      ).withPinnedSession
    )
    Await.result(fResult, 1.second.toConcurrent).sortBy(_.fromTime.toInstant).map(_.id0.searchIndex0)
  }

  test("Search 1") {
    val v0 = v(0, 0.0)
    val data = runSearch(
      rawDatas.merge(v0)
    )(0.hoursToInstant, 1.hoursToInstant)
    data shouldBe Seq(v0)
  }

  test("Search 2") {
    val v0 = v(0, 0.0, 60, 0)
    val v1 = v(60, 1.0, 5000, 1.0)
    val v2 = v(100000, 2.0, 1, 2.0)
    val data = runSearch(
      rawDatas.merge(v0),
      rawDatas.merge(v1),
      rawDatas.merge(v2)
    )(0.minutesToInstant, 5000.minutesToInstant)
    data shouldBe Seq(v0, v1)
  }

  test("Search two tinys in large range") {
    val v0 = v(1000, 0.0)
    val v1 = v(1000000, 1.0)
    val data = runSearch(
      rawDatas.merge(v0),
      rawDatas.merge(v1)
    )(1000.minutesToInstant, 1000001.minutesToInstant)
    data shouldBe Seq(v0, v1)
  }

  test("Huge in tiny range") {
    val v0 = v(0, 0.0, 10 * 365 * 24 * 60)
    val data = runSearch(
      rawDatas.merge(v0)
    )(10000.minutesToInstant, 10001.minutesToInstant)
    data shouldBe Seq(v0)
  }

}
