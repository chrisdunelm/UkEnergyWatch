package org.ukenergywatch.db

import org.scalatest._
import org.joda.time._

class BmUnitFpnSpec extends TestBaseDb {
  import TestDb.dal._
  import TestDb.dal.profile.simple._

  object BmUnitFpnSpec {
    implicit class RichBmUnitFpn(val v: BmUnitFpn) {
      def id0: BmUnitFpn = v.copy(id = 0)
    }
    implicit class RichBmUnitFpnSeq(val vs: Seq[BmUnitFpn]) {
      def id0: Seq[BmUnitFpn] = vs.map(_.id0)
    }
  }
  import BmUnitFpnSpec._

  def mergeIn(items: BmUnitFpn*)(implicit session: Session) {
    for (item <- items) {
      BmUnitFpns.mergeInsert(item)
    }
  }

  def all()(implicit session: Session): Seq[BmUnitFpn] = {
    val q = BmUnitFpns.sortBy(_.fromTime)
    val result = q.list.map(_.copy(id = 0))
    result.toSeq
  }

  def t(time: Int): ReadableInstant = new Instant(time * 1000L)
  def i(t0: Int, t1: Int): ReadableInterval = new Interval(t(t0), t(t1))

  def a(time: Int, mw0: Float, mw1: Float) = BmUnitFpn("a", time, mw0, time + 1, mw1)
  def a(time0: Int, mw0: Float, time1: Int, mw1: Float) = BmUnitFpn("a", time0, mw0, time1, mw1)

  "BmUnitFpns" should "insert a single item" in prepare { implicit session =>
    mergeIn(BmUnitFpn("a", 10, 1, 20, 2))
    all() shouldBe Seq(BmUnitFpn("a", 10, 1, 20, 2))
  }

  it should "check canMerge" in {
    val a = BmUnitFpn("a", 10, 1, 20, 1)
    val b = BmUnitFpn("a", 20, 2, 30, 2)
    a.canMerge(b) shouldBe false
  }
  
  it should "insert two non-adjacent items" in prepare { implicit session =>
    mergeIn(BmUnitFpn("a", 10, 1, 20, 2), BmUnitFpn("a", 30, 2, 40, 3))
    all() shouldBe Seq(BmUnitFpn("a", 10, 1, 20, 2), BmUnitFpn("a", 30, 2, 40, 3))
  }
  
  it should "not merge two adjacent items with different mw" in prepare { implicit session =>
    mergeIn(BmUnitFpn("a", 10, 1, 20, 1), BmUnitFpn("a", 20, 2, 30, 2))
    all() shouldBe Seq(BmUnitFpn("a", 10, 1, 20, 1), BmUnitFpn("a", 20, 2, 30, 2))
  }
  
  it should "merge two adjacent items with same mw" in prepare { implicit session =>
    mergeIn(BmUnitFpn("a", 10, 1, 20, 1), BmUnitFpn("a", 20, 1, 30, 1))
    all() shouldBe Seq(BmUnitFpn("a", 10, 1, 30, 1))
  }
  
  it should "merge two adjacent items with same mw backwards" in prepare { implicit session =>
    mergeIn(BmUnitFpn("a", 20, 1, 30, 1), BmUnitFpn("a", 10, 1, 20, 1))
    all() shouldBe Seq(BmUnitFpn("a", 10, 1, 30, 1))
  }

  it should "merge between two compatible items" in prepare { implicit session =>
    mergeIn(a(1, 1, 1), a(3, 1, 1), a(2, 1, 1))
    all() shouldBe Seq(a(1, 1, 4, 1))
  }

  it should "merge backwards between two items" in prepare { implicit session =>
    mergeIn(a(1, 1, 1), a(3, 1, 2), a(2, 1, 1))
    all() shouldBe Seq(a(1, 1, 3, 1), a(3, 1, 2))
  }
  
  it should "merge forwards between two items" in prepare { implicit session =>
    mergeIn(a(1, 1, 2), a(3, 1, 1), a(2, 1, 1))
    all() shouldBe Seq(a(1, 1, 2), a(2, 1, 4, 1))
  }

  it should "get a spot value" in prepare { implicit session =>
    mergeIn(a(1, 10, 10), a(2, 20, 4, 40))
    BmUnitFpns.getSpot("a", t(0)) shouldBe None
    BmUnitFpns.getSpot("a", t(1)) shouldBe Some(10.0)
    BmUnitFpns.getSpot("a", t(2)) shouldBe Some(20.0)
    BmUnitFpns.getSpot("a", t(3)) shouldBe Some(30.0)
    BmUnitFpns.getSpot("a", t(4)) shouldBe None
    BmUnitFpns.getSpot("a", t(5)) shouldBe None
  }

  it should "get a range value" in prepare { implicit session =>
    mergeIn(a(1, 10, 10), a(2, 20, 4, 40))
    BmUnitFpns.getRange("a", i(0, 1)).id0 shouldBe Seq()
    BmUnitFpns.getRange("a", i(1, 2)).id0 shouldBe Seq(a(1, 10, 10))
    BmUnitFpns.getRange("a", i(2, 3)).id0 shouldBe Seq(a(2, 20, 4, 40))
    BmUnitFpns.getRange("a", i(3, 4)).id0 shouldBe Seq(a(2, 20, 4, 40))
    BmUnitFpns.getRange("a", i(4, 5)).id0 shouldBe Seq()
    BmUnitFpns.getRange("a", i(1, 4)).id0 shouldBe Seq(a(1, 10, 10), a(2, 20, 4, 40))
  }
  
}
