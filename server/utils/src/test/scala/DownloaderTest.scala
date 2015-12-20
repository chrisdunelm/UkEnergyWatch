package org.ukenergywatch.utils

import org.scalatest._

import org.ukenergywatch.utils.StringExtensions._
import org.ukenergywatch.utils.JavaTimeExtensions._
import scala.concurrent.{ Future, Await }

import scala.concurrent.ExecutionContext.Implicits.global

class DownloaderTest extends FunSuite with Matchers {

  def await[T](f: Future[T]): T = Await.result(f, 1.second.toConcurrent)

  test("Fake downloader works") {
    object Components extends DownloaderFakeComponent
    val dl = Components.downloader

    dl.setStrings(Map("a" -> "test_a", "b" -> "test_b"))

    await(dl.get("a")).toStringUtf8 shouldBe "test_a"
    await(dl.get("b")).toStringUtf8 shouldBe "test_b"
    an [Exception] should be thrownBy await(dl.get("c"))
  }

}
