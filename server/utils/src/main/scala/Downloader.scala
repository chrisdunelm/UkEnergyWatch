package org.ukenergywatch.utils

import dispatch._
import org.ukenergywatch.utils.StringExtensions._
import scala.concurrent.ExecutionContext

trait DownloaderComponent {

  def downloader: Downloader

  trait Downloader {
    def get(url: String)(implicit ec: ExecutionContext): Future[Array[Byte]]
  }

}

trait DownloaderRealComponent extends DownloaderComponent {

  lazy val downloader = new DownloaderReal

  class DownloaderReal extends Downloader {
    def get(getUrl: String)(implicit ec: ExecutionContext): Future[Array[Byte]] = {
      Http(url(getUrl) OK as.Bytes)
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

    def get(url: String)(implicit ec: ExecutionContext): Future[Array[Byte]] = {
      content.get(url) match {
        case Some(data) => Future.successful(data)
        case None =>
          //println(s"No data for url: '$url'")
          Future.failed(new Exception)
      }
    }
  }

}
