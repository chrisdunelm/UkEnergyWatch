package org.ukenergywatch.utils

import java.nio.charset.Charset

object StringExtensions {

  val utf8 = Charset.forName("UTF-8")

  implicit class RichString(val s: String) extends AnyVal {
    def toBytesUtf8: Array[Byte] = s.getBytes(utf8)
  }

  implicit class RichByteArray(val b: Array[Byte]) extends AnyVal {
    def toStringUtf8: String = new String(b, utf8)
  }

}
