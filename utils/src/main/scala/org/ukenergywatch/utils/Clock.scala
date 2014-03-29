package org.ukenergywatch.utils

import org.joda.time._

trait ClockComp {
  def clock: Clock
  trait Clock {
    def nowUtc(): DateTime
  }
}

trait RealClockComp extends ClockComp {
  def clock: Clock = RealClock
  object RealClock extends Clock {
    def nowUtc(): DateTime = DateTime.now(DateTimeZone.UTC)
  }
}

trait FakeClockComp extends ClockComp {
  lazy val clock: FakeClock = new FakeClock
  class FakeClock extends Clock {
    private var now: DateTime = new DateTime(2000, 1, 1, 0, 0, DateTimeZone.UTC)
    def set(now: DateTime): Unit = this.now = now
    def nowUtc(): DateTime = now
  }
}
