package org.ukenergywatch.db

import java.time.Instant
import org.ukenergywatch.utils.RangeOfValue

case class RawData(
  rawDataType: RawDataType,
  name: String,
  fromTime: DbTime,
  toTime: DbTime,
  fromValue: Double,
  toValue: Double,
  id: Int = 0
) extends MergeableValue with RangeOfValue[Instant, Double] {
  def id0: RawData = copy(id = 0)
  def value0: Double = fromValue
  def value1: Double = toValue
}

trait RawDataTable extends Mergeable{
  val driver: slick.driver.JdbcDriver
  import driver.api._

  class RawDatas(tag: Tag) extends Table[RawData](tag, "rawdata") with MergeableTable {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def rawDataType = column[RawDataType]("rawDataType")
    def name = column[String]("name")
    def fromTime = column[DbTime]("fromTime")
    def toTime = column[DbTime]("toTime")
    def fromValue = column[Double]("fromValue")
    def toValue = column[Double]("toValue")
    def * = (rawDataType, name, fromTime, toTime, fromValue, toValue, id) <> (RawData.tupled, RawData.unapply)
  }

  object rawDatas extends TableQuery[RawDatas](new RawDatas(_)) with MergeQuery[RawData, RawDatas] {
    protected def mergeFilter(item: RawData) = { x =>
      x.rawDataType === item.rawDataType && x.name === item.name &&
      x.fromValue === item.fromValue && x.toValue === item.toValue &&
      item.toValue == item.fromValue
    }
  }

}
