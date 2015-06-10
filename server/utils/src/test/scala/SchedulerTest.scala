package org.ukenergywatch.utils

import org.scalatest._

import java.util.concurrent.ScheduledExecutorService
import org.joda.time.ReadableDuration

import org.ukenergywatch.utils.JodaExtensions._

class SchedulerTest extends FunSuite with Matchers {

  test("Scheduler works") {
    object Sch extends SchedulerFakeComponent with ClockFakeComponent

    var test = -1
    Sch.scheduler.run(1.minute, 10.seconds) { iteration =>
      test = iteration
      if (iteration == 0) {
        ReAction.Retry(10.seconds)
        //ReAction.Success
      } else {
        ReAction.Success
      }
    }

    Sch.clock.nowUtc2 += 5.seconds
    test shouldBe -1
    Sch.clock.nowUtc2 += 10.seconds
    test shouldBe 0
    Sch.clock.nowUtc2 += 10.seconds
    test shouldBe 1
    Sch.clock.nowUtc2 += 10.seconds
    test shouldBe 1
    Sch.clock.nowUtc2 += 30.seconds
    test shouldBe 1
    Sch.clock.nowUtc2 += 10.seconds
    test shouldBe 0
  }

  test ("Realtime scheduler works") {
    object Sch extends SchedulerRealtimeComponent with ClockRealtimeComponent

    var test = -1
    val now = Sch.clock.nowUtc()
    Sch.scheduler.schedule(now + 200.millis) { () => test = 0 }
    Thread.sleep(100)
    test shouldBe -1
    Thread.sleep(200)
    test shouldBe 0
  }

}
