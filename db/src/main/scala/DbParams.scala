package org.ukenergywatch.db

import org.ukenergywatch.utils.FlagsComponent

trait DbParamsComponent {
  def dbParams: DbParams
  trait DbParams {
    def host: String
    def database: String
    def user: String
    def password: String
  }
}

trait DbFlagParamsComponent extends DbParamsComponent {
  this: FlagsComponent =>
  object dbParams extends DbParams {
    object Flags extends FlagsBase {
      val dbHost = flag[String](name = "dbHost")
      val dbDatabase = flag[String](name = "dbDatabase")
      val dbUser = flag[String](name = "dbUser")
      val dbPassword = flag[String](name = "dbPassword")
    }
    def host: String = Flags.dbHost()
    def database: String = Flags.dbDatabase()
    def user: String = Flags.dbUser()
    def password: String = Flags.dbPassword()
  }
  dbParams.Flags // Early flags initialisation
}
