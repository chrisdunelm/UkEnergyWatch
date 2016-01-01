package org.ukenergywatch.utils

import org.scalatest._
import scala.collection.JavaConversions._
import org.ukenergywatch.utils.JavaTimeExtensions._
import org.ukenergywatch.utils.FileExtensions._
import org.ukenergywatch.utils.CollectionExtensions._
import java.nio.file.Files

class LogTest extends FunSuite with Matchers {

  test("memory logging") {
    object App extends LogMemoryComponent with ClockFakeComponent
    App.clock.fakeInstant = 0.secondsToInstant
    App.log.info("message")
    App.log.msgs should have size 1
    App.log.msgs(0) should include regex "1970-01-01T00:00:00Z.*Info.*message"
  }

  // This test uses a temporary directory
  test("file logging with rotation") {
    val tempDir = Files.createTempDirectory("LogTest")
    try {
      object App extends LogFileComponent with ClockFakeComponent with FlagsComponent
      App.clock.fakeInstant = 0.secondsToInstant
      App.flags.parse(s"--logDirectoryPath=$tempDir --logFileSize=1 --logFileCount=2 --logFilenamePrefix=test")
      App.log.info("message1")
      App.log.info("message2")
      App.log.info("message3")
      App.log.info("message4")
      Files.list(tempDir).count shouldBe 3
      Files.readAllLines(tempDir.resolve("test.log")).toSeq.only should include regex
        "1970-01-01T00:00:00Z.*Info.*message4"
      Files.readAllLines(tempDir.resolve("test.0000.log")).toSeq.only should include regex
        "1970-01-01T00:00:00Z.*Info.*message3"
      Files.readAllLines(tempDir.resolve("test.0001.log")).toSeq.only should include regex
        "1970-01-01T00:00:00Z.*Info.*message2"
    } finally {
      tempDir.subDelete()
    }
  }

}
