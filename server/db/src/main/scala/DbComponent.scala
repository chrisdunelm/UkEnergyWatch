package org.ukenergywatch.db

import slick.driver.{JdbcDriver, H2Driver}
import java.time.Duration
import org.ukenergywatch.utils.JavaTimeExtensions._
import scala.concurrent.Await

trait DbComponent {

  val db: Db

  trait Db
      extends AggregateTable
      with AggregateProgressTable
      with RawDataTable
      with RawProgressTable
  {
    val driver: JdbcDriver
    def db: driver.api.Database

    import driver.api._

    def createTables: DBIO[Unit] = {
      rawDatas.schema.create >>
      rawProgresses.schema.create >>
      aggregates.schema.create >>
      aggregateProgresses.schema.create
    }

    def executeAndWait[T](action: DBIO[T], timeout: Duration): T = {
      val future = db.run(action)
      Await.result(future, timeout.toConcurrent)
    }
  }

}

trait DbMemoryComponent extends DbComponent {

  lazy val db = new DbMemory

  class DbMemory extends Db {
    lazy val driver = H2Driver
    lazy val db = driver.api.Database.forURL(
      "jdbc:h2:mem:",
      driver = "org.h2.Driver"
    )
  }

}

trait DbPersistentMemoryComponent extends DbComponent {
  object db extends Db {
    private val name = java.util.UUID.randomUUID().toString
    lazy val driver = H2Driver
    lazy val db = driver.api.Database.forURL(
      s"jdbc:h2:mem:$name;DB_CLOSE_DELAY=60", // Keep DB around for 60 seconds
      driver = "org.h2.Driver"
    )
  }
}
