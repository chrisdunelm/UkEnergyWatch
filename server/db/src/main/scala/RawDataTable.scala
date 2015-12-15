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
  searchIndex: Int = -1,
  id: Int = 0
) extends MergeableValue with SearchableValue with RangeOfValue[Instant, Double] {
  def id0: RawData = copy(id = 0)
  def searchIndex0: RawData = copy(searchIndex = -1)
  def value0: Double = fromValue
  def value1: Double = toValue
  def withSearchIndex(searchIndex: Int): this.type = copy(searchIndex = searchIndex).asInstanceOf[this.type]
}

trait RawDataTable extends Mergeable with Searchable {
  val driver: slick.driver.JdbcDriver
  import driver.api._

  class RawDatas(tag: Tag) extends Table[RawData](tag, "rawdata") with MergeableTable with SearchableTable {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def rawDataType = column[RawDataType]("rawDataType")
    def name = column[String]("name")
    def fromTime = column[DbTime]("fromTime")
    def toTime = column[DbTime]("toTime")
    def fromValue = column[Double]("fromValue")
    def toValue = column[Double]("toValue")
    def searchIndex = column[Int]("searchIndex")
    def * = (rawDataType, name, fromTime, toTime, fromValue, toValue, searchIndex, id) <> (
      RawData.tupled, RawData.unapply)

    // For general searches (might not be required)
    def indexSearch = index("idx_search", searchIndex)
    // For merges
    def indexMerge = index("idx_merge", (rawDataType, name, searchIndex))
  }

  object rawDatas extends TableQuery[RawDatas](new RawDatas(_))
      with MergeQuery[RawData, RawDatas] with SearchQuery[RawData, RawDatas] {
    protected def mergeFilter(item: RawData) = { x =>
      x.rawDataType === item.rawDataType && x.name === item.name &&
      x.fromValue === item.fromValue && x.toValue === item.toValue &&
      item.toValue == item.fromValue // Really is meant to be '==', not '==='
    }
  }

}
