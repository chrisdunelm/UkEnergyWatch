package org.ukenergywatch.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory

trait Slogger {

  lazy val log: Logger = LoggerFactory.getLogger(this.getClass)

}
