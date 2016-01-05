package org.ukenergywatch.appimporter

import org.ukenergywatch.importers.{ ImportControlComponent, AggregateControlComponent,
  ElectricImportersComponent }
import org.ukenergywatch.data.DataComponent
import org.ukenergywatch.db.{ DbComponent, DbMysqlComponent,
  DbParamsComponent, DbFlagParamsComponent, DbPersistentMemoryComponent }
import org.ukenergywatch.utils.{ LogFileComponent, ClockRealtimeComponent, FlagsComponent,
  ElexonParamsComponent, DownloaderRealComponent, SchedulerComponent, SchedulerRealtimeComponent,
  LogComponent, ReAction, ElexonFlagParamsComponent, LogMemoryComponent }
import org.ukenergywatch.utils.JavaTimeExtensions._
import java.time.Duration

import scala.concurrent.ExecutionContext.Implicits.global

object AppImporter {

  sealed trait ImportType
  object ImportType {
    case object All extends ImportType
    case object ElectricFuelInst extends ImportType
    case object ElectricFrequency extends ImportType
    case object ElectricActualGeneration extends ImportType
  }

  def main(args: Array[String]): Unit = {

    trait AppComponent {
      this: FlagsComponent
          with SchedulerComponent
          with ImportControlComponent
          with AggregateControlComponent
          with LogComponent =>

      object Flags extends FlagsBase {
        val disableElectricFuelInst = flag[Boolean](name = "disableElectricFuelInst", defaultValue = false)
        val disableElectricFrequency = flag[Boolean](name = "disableElectricFrequency", defaultValue = false)
        val disableElectricActualGeneration =
          flag[Boolean](name = "disableElectricActualGeneration", defaultValue = false)
        val enableOnly = flag[ImportType](name = "enableOnly", defaultValue = ImportType.All)
      }
      Flags // Early initialise

      private def catchAll(errorPrefix: String)(fn: => Unit): Int => ReAction = {
        retry: Int => {
          try {
            fn
          } catch {
            case t: Throwable => log.error(s"$errorPrefix: $t")
          }
          ReAction.Success
        }
      }

      private def scheduleElectricFuelInst(): Unit = {
        // Every 5 minutes, 1 minute offset for real-time.
        // Every 5 minutes, 3.5 minute offset for past-only.
        scheduler.run(5.minutes, 66.seconds)(catchAll("fuelInst import error (realtime)") {
          importControl.fuelInst(false, 2.minutes)
        })
        scheduler.run(5.minutes, 3.5.minutes)(catchAll("fuelInst import error (past-only)") {
          importControl.fuelInst(true, 2.minutes)
        })
        // Aggregate
        scheduler.run(5.minutes, 30.seconds)(catchAll("fuelInst aggregate error") {
          aggregateControl.fuelInst(10, 1.minute)
        })
      }

      private def scheduleElectricFrequency(): Unit = {
        // Every 2 minutes, 1 second offset for real-time.
        // Every 2 minutes, 61 second offset for past-only.
        scheduler.run(2.minutes, 1.second)(catchAll("frequency import error (realtime)") {
          importControl.freq(false, 55.seconds)
        })
        scheduler.run(2.minutes, 61.seconds)(catchAll("frequency import error(past-only)") {
          importControl.freq(true, 55.seconds)
        })
        // Aggregate
        scheduler.run(4.minutes, 0.seconds)(catchAll("frequency aggregate error") {
          aggregateControl.frequency(10, 1.minute)
        })
      }

      private def scheduleElectricActualGeneration(): Unit = {
        // Every 10 minutes, 2 minute offset for real-time
        // Every 10 minutes, 7 minute offset for past-only
        scheduler.run(10.minutes, 2.minutes)(catchAll("actualGeneration import error(realtime)") {
          importControl.actualGeneration(false, 2.minutes)
        })
        scheduler.run(10.minutes, 7.minutes)(catchAll("actualGeneration import error(past-only)") {
          importControl.actualGeneration(true, 2.minutes)
        })
        // Aggregate
        scheduler.run(30.minutes, 3.minutes)(catchAll("actualGeneration aggregate error") {
          aggregateControl.actualGeneration(10, 1.minute)
        })
      }

      def run(): Unit = {
        log.info("AppImporter starting")
        def enabled(flag: FlagsBase.Flag[Boolean], importType: ImportType): Boolean = {
          !flag() && (Flags.enableOnly() == ImportType.All || Flags.enableOnly() == importType)
        }
        /*// Schedule actual generation import. Every 5 minutes, 1 minute offset
        scheduler.run(5.minutes, 78.seconds) { retry =>
          try {
            importControl.actualGeneration(4.minutes)
          } catch {
            case t: Throwable => log.error(s"actualGeneration import error: $t", t)
          }
          ReAction.Success
        }*/
        if (enabled(Flags.disableElectricFuelInst, ImportType.ElectricFuelInst)) {
          log.info("Scheduling ElectricFuelInst")
          scheduleElectricFuelInst()
        }
        if (enabled(Flags.disableElectricFrequency, ImportType.ElectricFrequency)) {
          log.info("Scheduling ElectricFrequency")
          scheduleElectricFrequency()
        }
        if (enabled(Flags.disableElectricActualGeneration, ImportType.ElectricActualGeneration)) {
          log.info("Scheduling ElectricActionGeneration")
          scheduleElectricActualGeneration()
        }
        log.info("AppImporter imports scheduled")
      }
    }

    trait InitFlagsTemplate extends FlagsComponent {
      object InitFlags extends FlagsBase {
        val useMemoryDbAndLog = flag[Boolean](name = "useMemoryDbAndLog", defaultValue = false)
      }
      InitFlags
    }
    object InitFlags extends InitFlagsTemplate
    InitFlags.flags.parse(args, allowUnknownFlags = true)

    trait AppTemplate extends AppComponent
        with InitFlagsTemplate // Only so --useMemoryDbAndLog is recognised as a valid flag
        with ImportControlComponent
        with AggregateControlComponent
        with ElectricImportersComponent
        with ElexonFlagParamsComponent
        with DataComponent
        with DbComponent
        with LogComponent
        with DownloaderRealComponent
        with ClockRealtimeComponent
        with FlagsComponent
        with SchedulerRealtimeComponent
    val app = if (InitFlags.InitFlags.useMemoryDbAndLog()) {
      println("In-memory configuration")
      trait DbPersistentMemoryComponent1Hour extends DbPersistentMemoryComponent {
        override def dbPersistentMemoryCloseDelay: Duration = 1.hour
      }
      object App extends AppTemplate
          with DbPersistentMemoryComponent1Hour
          with LogMemoryComponent
      App.db.executeAndWait(App.db.createTables, 1.second)
      App
    } else {
      println("Real database configuration")
      object App extends AppTemplate
          with DbMysqlComponent
          with DbFlagParamsComponent
          with LogFileComponent
      App
    }

    println("AppImporter")
    app.flags.parse(args)
    app.run()

    println("Type 'exit' and press enter to exit...")
    var exit = false
    while (!exit) {
      val r = io.StdIn.readLine()
      if (r == "exit") {
        exit = true
      }
    }
  }

}
