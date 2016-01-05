package org.ukenergywatch.utils

import java.io.InputStream

object StreamExtensions {

  implicit class RichInputStream(val s: InputStream) extends AnyVal {

    def toByteArray: Array[Byte] = {
      Iterator.continually(s.read()).takeWhile(_ != -1).map(_.toByte).toArray
    }

  }

}
