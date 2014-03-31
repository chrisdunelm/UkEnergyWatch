package org.ukenergywatch.db

import org.scalatest._

class GridFrequencySpec extends TestBaseDb {
  import TestDb.dal._
  import TestDb.dal.profile.simple._

  def all()(implicit session: Session): Seq[GridFrequency] =
    GridFrequencies.sortBy(_.endTime).list

  "GridFrequencies" should "insert two different initial items" in prepare { implicit session =>
    val f1 = GridFrequency(10, 1.1f)
    val f2 = GridFrequency(11, 1.2f)
    GridFrequencies.insert(f1)
    GridFrequencies.insert(f2)
    all() shouldBe Seq(f1, f2)
  }

  it should "insert one of two identical items" in prepare { implicit session =>
    val f1 = GridFrequency(10, 1.1f)
    val f2 = GridFrequency(10, 1.2f)
    GridFrequencies.insert(f1)
    GridFrequencies.insert(f2)
    all() shouldBe Seq(f1)
  }

}
