package org.ukenergywatch.slogger.impl

import org.slf4j.ILoggerFactory
import org.slf4j.Logger
import scala.collection.concurrent

class LoggerFactory extends ILoggerFactory {

  val map = concurrent.TrieMap[String, Logger]()

  def getLogger(name: String): Logger = {
    map.getOrElseUpdate(name, new Slogger(name))
  }

}
