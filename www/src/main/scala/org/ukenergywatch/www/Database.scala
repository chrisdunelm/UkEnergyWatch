package org.ukenergywatch.www

import org.ukenergywatch.db.DalComp

trait MysqlDalComp extends DalComp {
  val dal = MysqlDal
  object MysqlDal extends Dal {
    val profile = scala.slick.driver.MySQLDriver
    import Flags._
    val database = profile.simple.Database.forURL(s"jdbc:mysql://${mysqlHost()}/${mysqlDatabase()}", mysqlUser(), mysqlPassword(), driver = "com.mysql.jdbc.Driver")
  }
}

object Database extends MysqlDalComp