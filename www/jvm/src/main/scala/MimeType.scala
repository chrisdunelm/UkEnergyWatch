package org.ukenergywatch.www

object MimeType {

  val extensionMap: Map[String, String] = Map(
    "jpeg" -> "image/jpeg",
    "jpg" -> "image/jpeg",
    "js" -> "text/javascript",
    "png" -> "image/png",
    "map" -> "application/octet-stream"
  )

  def fromFilename(filename: String): Option[String] = {
    val dotIndex = filename.lastIndexOf('.')
    if (dotIndex >= 0) {
      val ext = filename.substring(dotIndex + 1)
      extensionMap.get(ext)
    } else {
      None
    }
  }

}
