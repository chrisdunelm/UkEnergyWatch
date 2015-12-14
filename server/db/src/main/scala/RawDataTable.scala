package org.ukenergywatch.db

import java.time.Instant
import org.ukenergywatch.utils.RangeOfValue

// searchIndex is a special int value that allows fast retreival of data
// within a specified time-range.
// A tree-structure is used.
// The first level has each time-period being 2 hours, overlapping by half (ie 1 hour)
// Each level multiplies the time-period by 10
// level 1 = 2 hours
// level 2 = 20 hours
// level 3 = 200 hours = ~8 days
// level 4 = 2000 hours = ~83 days = ~3 months
// level 5 = 20000 hours = ~2 years
// level 6 = 200000 hours = ~22 years (should be enough, can always add another layer)
//
// Each level starts at a fixed offset, that divides by 10 for each level increment
// level 1 offset = 0
// level 2 offset = 10,000,000 (allowing ~1000 years of level 1 values)
// level 3 offset = 11,000,000
// level 4 offset = 11,100,000
// level 5 offset = 11,110,000
// level 6 offset = 11,111,000
//
// Writes use a searchIndex value that completely covers the range of the value, in the lowest level possible.
// Reads must do a range lookup in every level that selects all ranges that may contain the required data
// E.g.
// select * from rawdata where (
//   (searchIndex >= 0 && searchIndex <= 1) OR
//   (searchIndex >= 10000000 && searchIndex <= 10000001) OR
//   (searchIndex >= 11000000 && searchIndex <= 11000001) OR
//   ...
// )

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
  def searchIndex0 = copy(searchIndex = -1)
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
    def searchIndexIndex = index("idx_searchIndex", searchIndex)
    // For merges
    def mergeIndex = index("idx_mergeIndex", (rawDataType, name, searchIndex))
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
