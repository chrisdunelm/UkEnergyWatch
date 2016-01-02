package org.ukenergywatch.initmysql

import org.ukenergywatch.db.{ DbMysqlComponent, DbComponent, DbParamsComponent }
import org.ukenergywatch.utils.{ FlagsComponent }
import org.ukenergywatch.utils.JavaTimeExtensions._

object InitMysql {

  def main(args: Array[String]): Unit = {
    println("initmysql")

    trait AppComponent extends DbParamsComponent {
      this: FlagsComponent with DbComponent =>

      object app {
        object Flags extends FlagsBase {
          val mysqlHost = flag[String](name = "mysqlHost")
          val mysqlDatabase = flag[String](name = "mysqlDatabase")
          val mysqlUser = flag[String](name = "mysqlUser")
          val mysqlPassword = flag[String](name = "mysqlPassword")
        }
      }
      app.Flags // Early initialise flags object

      object dbParams extends DbParams {
        def host: String = app.Flags.mysqlHost()
        def database: String = app.Flags.mysqlDatabase()
        def user: String = app.Flags.mysqlUser()
        def password: String = app.Flags.mysqlPassword()
      }

      def run(): Unit = {
        println("About to create tables")
        db.executeAndWait(db.createTables, 15.seconds)
        println("Tables created")
      }
    }

    object App extends AppComponent
        with DbMysqlComponent
        with FlagsComponent

    App.flags.parse(args)
    App.run()

  }

}
