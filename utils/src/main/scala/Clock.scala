package org.ukenergywatch.utils

import org.ukenergywatch.utils.JavaTimeExtensions._
import java.time.{ LocalDateTime, Instant, ZoneOffset }

trait ClockComponent {

  def clock: Clock

  trait Clock {
    def utcClock: java.time.Clock

    def nowUtc(): Instant = Instant.now(utcClock)
  }

}

trait ClockRealtimeComponent extends ClockComponent {

  lazy val clock = new ClockRealtime

  class ClockRealtime extends Clock {
    val utcClock: java.time.Clock = Clock.utc
  }

}

trait ClockFakeComponent extends ClockComponent {

  lazy val clock = new ClockFake

  class ClockFake extends Clock {
    private var listeners = List[Instant => Unit]()
    def addListener(fn: Instant => Unit): Unit = listeners ::= fn

    private var fakeUtcClock: java.time.Clock = java.time.Clock.fixed(Instant.ofEpochSecond(0L), ZoneOffset.UTC)

    def utcClock: java.time.Clock = fakeUtcClock

    def setUtcClock(clock: java.time.Clock): Unit = {
      fakeUtcClock = clock
      val now = nowUtc()
      for (listener <- listeners) {
        listener(now)
      }
    }

    def fakeInstant: Instant = nowUtc()
    def fakeInstant_=(instant: Instant): Unit = {
      setUtcClock(java.time.Clock.fixed(instant, ZoneOffset.UTC))
    }
  }

}
