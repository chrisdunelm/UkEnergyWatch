package org.ukenergywatch.appimporter

import org.ukenergywatch.importers.{ ImportControlComponent, ElectricImportersComponent }
import org.ukenergywatch.data.DataComponent
import org.ukenergywatch.db.{ DbMysqlComponent, DbParamsComponent }
import org.ukenergywatch.utils.{ LogFileComponent, ClockRealtimeComponent, FlagsComponent,
  ElexonParamsComponent, DownloaderRealComponent, SchedulerComponent, SchedulerRealtimeComponent,
  LogComponent, ReAction }
import org.ukenergywatch.utils.JavaTimeExtensions._

import scala.concurrent.ExecutionContext.Implicits.global

object AppImporter {

  def main(args: Array[String]): Unit = {

    trait AppComponent extends ElexonParamsComponent with DbParamsComponent {
      this: FlagsComponent with SchedulerComponent with ImportControlComponent with LogComponent =>

      object app {
        object Flags extends FlagsBase {
          val elexonKey = flag[String](name = "elexonKey")
          val mysqlHost = flag[String](name = "mysqlHost", defaultValue = "127.0.0.1")
          val mysqlDatabase = flag[String](name = "mysqlDatabase", defaultValue = "ukenergywatch2")
          val mysqlUser = flag[String](name = "mysqlUser")
          val mysqlPassword = flag[String](name = "mysqlPassword")
        }
      }
      app.Flags // Early initialise flags object

      object elexonParams extends ElexonParams {
        def key: String = app.Flags.elexonKey()
      }
      object dbParams extends DbParams {
        def host: String = app.Flags.mysqlHost()
        def database: String = app.Flags.mysqlDatabase()
        def user: String = app.Flags.mysqlUser()
        def password: String = app.Flags.mysqlPassword()
      }

      def run() {
        // Schedule actual generation import. Every 5 minutes, 1 minute offset
        scheduler.run(5.minutes, 78.seconds) { retry =>
          try {
            importControl.actualGeneration(4.minutes)
          } catch {
            case t: Throwable => log.error(s"actualGeneration import error: $t")
          }
          ReAction.Success
        }
        // Schedule fuel-inst (generation by fuel type). Every 2.5 minutes, 1 minute offset
        scheduler.run(2.5.minutes, 66.seconds) { retry =>
          try {
            importControl.fuelInst(2.minutes)
          } catch {
            case t: Throwable => log.error(s"fuelInst import error: $t")
          }
          ReAction.Success
        }
        // Schedule frequency. Every 1 minutes, 10 second offset
        scheduler.run(1.minute, 10.seconds) { retry =>
          try {
            importControl.freq(55.seconds)
          } catch {
            case t: Throwable => log.error(s"frequency import error: $t")
          }
          ReAction.Success
        }
      }
    }

    object App extends AppComponent
        with ImportControlComponent
        with ElectricImportersComponent
        with DataComponent
        with DbMysqlComponent
        with DbParamsComponent
        with LogFileComponent
        with DownloaderRealComponent
        with ClockRealtimeComponent
        with FlagsComponent
        with SchedulerRealtimeComponent

    println("AppImporter")
    App.flags.parse(args)
    App.run()

    println("Press enter to exit...")
    io.StdIn.readLine()
  }

}
