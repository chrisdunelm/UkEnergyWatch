package org.ukenergywatch.data

import org.ukenergywatch.db.DbComponent
import org.ukenergywatch.db.RawData

object TimeMerge {

  //def merge(table: TableQuery[Progresses], item: Progress): Unit = {
  //  val q = table.result
  //}

}

trait DataComponent {
  this: DbComponent =>

  lazy val data = new Data

  class Data {

    def write(rawData: Seq[RawData]): Unit = {
      
    }

  }

}

/*
trait DataModule {
  // Provides
  val dataReader = wire[DataReader]
  val dataWriter = wire[DataWriter]

  // Dependencies
  def tables: Tables
}

class DataReader(tables: Tables) {

}

class DataWriter(tables: Tables) {

  def write(): Unit = {
  }

}
 */
