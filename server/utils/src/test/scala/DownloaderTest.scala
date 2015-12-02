package org.ukenergywatch.utils

import org.scalatest._

import org.ukenergywatch.utils.StringExtensions._

class DownloaderTest extends FunSuite with Matchers {

  test("Fake downloader works") {
    object Components extends DownloaderFakeComponent
    val dl = Components.downloader

    dl.setStrings(Map("a" -> "test_a", "b" -> "test_b"))

    dl.get("a").map(_.toStringUtf8) shouldBe Some("test_a")
    dl.get("b").map(_.toStringUtf8) shouldBe Some("test_b")
    dl.get("c").map(_.toStringUtf8) shouldBe None
  }

}
