package org.ukenergywatch.utils

import org.joda.time.{DateTime, DateTimeZone}

trait ClockComponent {

  def clock: Clock

  trait Clock {
    def nowUtc(): DateTime
  }

}

trait ClockRealtimeComponent extends ClockComponent {

  lazy val clock = new ClockRealtime

  class ClockRealtime extends Clock {
    def nowUtc(): DateTime = DateTime.now(DateTimeZone.UTC)
  }

}

trait ClockFakeComponent extends ClockComponent {

  lazy val clock = new ClockFake

  class ClockFake extends Clock {
    var listeners = List[DateTime => Unit]()
    def addListener(fn: DateTime => Unit): Unit = listeners ::= fn

    var now = new DateTime(2015, 1, 1, 0, 0, 0, DateTimeZone.UTC)
    def nowUtc: DateTime = now
    def nowUtc2: DateTime = now
    def nowUtc2_=(newNow: DateTime): Unit = {
      now = newNow
      for (listener <- listeners) {
        listener(now)
      }
    }
  }

}
