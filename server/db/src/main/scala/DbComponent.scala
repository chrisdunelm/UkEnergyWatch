package org.ukenergywatch.db

import slick.driver.{JdbcDriver, H2Driver}

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
      rawProgresses.schema.create
      // TODO: All tables
    }
  }

}

trait DbMemoryComponent extends DbComponent {

  lazy val db = new DbMemory

  class DbMemory extends Db {
    lazy val driver = H2Driver
    lazy val db = driver.api.Database.forURL(
      "jdbc:h2:mem:", driver="org.h2.Driver")
  }

}
