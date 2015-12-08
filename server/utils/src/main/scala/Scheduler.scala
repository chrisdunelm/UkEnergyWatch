package org.ukenergywatch.utils

import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit, Callable}
import org.ukenergywatch.utils.JavaTimeExtensions._
import java.time.{ Duration, Instant }

import scala.annotation.tailrec

sealed trait ReAction
object ReAction {
  case object Success extends ReAction // Successful, don't retry
  case object Failure extends ReAction // Failure, don't retry
  // Retry 'offset' after start of previous action. Retry immediately if this time has already passed
  case class Retry(offset: Duration) extends ReAction
}

trait SchedulerComponent {

  def scheduler: Scheduler

  trait Scheduler {
    // fn Int parameter is the retry count
    def run(period: Duration, offset: Duration)(fn: Int => ReAction): Unit
    def schedule(when: Instant)(fn: () => Unit): Unit
  }

}

trait SchedulerLogic {

  protected def nowUtc(): Instant
  def schedule(when: Instant)(fn: () => Unit): Unit

  def run(period: Duration, offset: Duration)(fn: Int => ReAction): Unit = {
    def periodLoop(nextTime: Instant): Unit = {
      def retryLoop(when: Instant, iteration: Int): Unit = {
        schedule(when) { () =>
          fn(iteration) match {
            case ReAction.Success | ReAction.Failure => periodLoop(nextTime + period)
            case ReAction.Retry(after) => retryLoop(when + after, iteration + 1)
          }
        }
      }
      retryLoop(nextTime, 0)
    }

    val now: Instant = nowUtc()
    val firstTime = {
      val base = now.alignTo(period) + offset
      if (base >= now) base else base + period
    }
    periodLoop(firstTime)
  }

}

trait SchedulerRealtimeComponent extends SchedulerComponent {
  this: ClockComponent =>

  lazy val scheduler = new SchedulerRealtime

  class SchedulerRealtime extends Scheduler with SchedulerLogic {
    val executor = Executors.newSingleThreadScheduledExecutor()
    protected def nowUtc(): Instant = clock.nowUtc()
    def schedule(when: Instant)(fn: () => Unit): Unit = {
      val now = clock.nowUtc()
      val runnable = new Runnable {
        def run(): Unit = fn()
      }
      if (when <= now) {
        executor.submit(runnable)
      } else {
        executor.schedule(runnable, (when - now).millis, TimeUnit.MILLISECONDS)
      }
    }
  }

}

trait SchedulerFakeComponent extends SchedulerComponent {
  this: ClockFakeComponent =>

  lazy val scheduler = new SchedulerFake

  class SchedulerFake extends Scheduler with SchedulerLogic {

    clock.addListener { now =>
      val (toRun, notYet) = all.partition(_._1 <= now)
      all = notYet
      for ((_, fn) <- toRun) {
        fn()
      }
    }

    protected def nowUtc(): Instant = clock.nowUtc()

    var all = List[(Instant, () => Unit)]()
    def schedule(when: Instant)(fn: () => Unit): Unit = {
      val now = clock.nowUtc()
      if (when <= now) {
        fn()
      } else {
        all ::= (when, fn)
      }
    }
  }

}

/*

What is needed:
Start an activity regularly at a fixed offset past each time-period
e.g. 30 seconds past every half-hour

The activity must either succees, or fail and specifiy when/whether to try again soon
If it needs to try again, then it specifies how long to wait, and how many times to try again

 */
