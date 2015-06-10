package org.ukenergywatch.db

import slick.driver.{JdbcDriver, H2Driver}

trait DbComponent {

  def db: Db

  trait Db extends DataTypes
      with AggregateTable
  {
    val driver: JdbcDriver
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
