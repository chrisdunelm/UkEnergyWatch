package org.ukenergywatch.db

import org.scalatest._

class DownloadSpec extends TestBaseDb {
  import TestDb.dal._
  import TestDb.dal.profile.simple._

  def all()(implicit session: Session): Seq[Download] = {
    val q = Downloads.sortBy(_.fromTime)
    q.list.map(_.copy(id = 0)).toSeq
  }

  "Downloads" should "insert two initial items" in prepare { implicit session =>
    val item = Download(0, 0, 1, 2)
    Downloads.mergeInsert(Download(0, 0, 1, 2))
    Downloads.mergeInsert(Download(0, 1, 1, 2))
    all().toSet shouldBe Set(Download(0, 0, 1, 2), Download(0, 1, 1, 2))
  }

}
