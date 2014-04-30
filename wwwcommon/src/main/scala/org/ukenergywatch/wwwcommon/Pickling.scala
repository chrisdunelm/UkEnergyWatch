package org.ukenergywatch.wwwcommon

import org.scalajs.spickling._

object Pickling {

  def register(): Unit = {
    // Register all classes used for data transfer

    PicklerRegistry.register[::[Any]]
    PicklerRegistry.register(Nil)

    PicklerRegistry.register[IndexUpdate]
    PicklerRegistry.register[GenByFuelUpdate]
  }

}
