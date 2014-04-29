package org.ukenergywatch.wwwcommon

import org.scalajs.spickling._

object Pickling {

  def register(): Unit = {
    // Register all classes used for data transfer
    PicklerRegistry.register[IndexUpdate]
  }

}
