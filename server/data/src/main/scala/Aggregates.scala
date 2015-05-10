package org.ukenergywatch.data

import com.softwaremill.macwire._
import org.ukenergywatch.db.Tables

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
