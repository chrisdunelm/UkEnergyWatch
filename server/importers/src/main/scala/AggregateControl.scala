package org.ukenergywatch.importers

import org.ukenergywatch.data.DataComponent
import org.ukenergywatch.db.DbComponent

trait AggregateControlComponent {
  this: DbComponent with DataComponent =>

  lazy val aggregateControl = new AggregateControl

  class AggregateControl {
    import db.driver.api._

  }

}
