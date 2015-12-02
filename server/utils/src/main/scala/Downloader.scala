package org.ukenergywatch.utils

import dispatch._
import org.ukenergywatch.utils.StringExtensions._

trait DownloaderComponent {

  def downloader: Downloader

  trait Downloader {
    def get(url: String): Option[Array[Byte]]
  }

}

trait DownloaderRealComponent extends DownloaderComponent {

  lazy val downloader = new DownloaderReal

  class DownloaderReal extends Downloader {
    def get(getUrl: String): Option[Array[Byte]] = {
      val request = url(getUrl)
      
      ???
    }
  }

}

trait DownloaderFakeComponent extends DownloaderComponent {

  lazy val downloader = new DownloaderFake

  class DownloaderFake extends Downloader {

    var content: Map[String, Array[Byte]] = Map()

    def set(newContent: Map[String, Array[Byte]]): Unit = {
      content = newContent
    }
    def setStrings(newContent: Map[String, String]): Unit = {
      content = newContent.mapValues(_.toBytesUtf8)
    }

    def get(url: String): Option[Array[Byte]] = {
      content.get(url)
    }
  }

}
