package org.ukenergywatch.importers

import org.ukenergywatch.db.DbComponent

trait GasImportersComponent {
  this: DbComponent =>

  lazy val gasImporters: GasImporters = new GasImporters

  class GasImporters {

    // TODO

  }

}
