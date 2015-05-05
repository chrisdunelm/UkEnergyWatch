package org.ukenergywatch.db

trait DbModuleMemory extends DbModule {
  lazy val driver = slick.driver.H2Driver
  lazy val db = driver.api.Database.forURL(
    "jdbc:h2:mem:", driver="org.h2.Driver")
}
