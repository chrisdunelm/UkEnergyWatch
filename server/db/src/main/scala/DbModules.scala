package org.ukenergywatch.db

import com.softwaremill.macwire._
import slick.driver.JdbcDriver

trait DbModule {
  // Provides
  lazy val tables = wire[Tables]

  // Dependencies
  val driver: JdbcDriver
  def db: driver.backend.Database
}

trait DbModuleMemory extends DbModule {
  lazy val driver = slick.driver.H2Driver
  lazy val db = driver.api.Database.forURL(
    "jdbc:h2:mem:", driver="org.h2.Driver")
}
