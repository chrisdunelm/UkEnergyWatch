package org.ukenergywatch.db

import org.scalatest._

trait TestBaseDb extends FlatSpec with Matchers {
  
  trait MemoryDalComp extends DalComp {
    val dal = MemoryDal
    object MemoryDal extends Dal {
      val profile = scala.slick.driver.H2Driver
      val database = profile.simple.Database.forURL("jdbc:h2:mem:", driver = "org.h2.Driver")
    }
  }

  object TestDb extends MemoryDalComp

  import TestDb.dal
  import TestDb.dal.profile.simple._

  def prepare(fn: Session => Unit): Unit = {
    dal.database withSession { session =>
      for (ddl <- dal.ddls) {
        ddl.create(session)
      }
      fn(session)
    }
  }

}
