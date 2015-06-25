package org.ukenergywatch.db

case class RawData(
  dataType: AggregationType,
  name: String,
  fromTime: DbTime,
  toTime: DbTime,
  fromValue: Double,
  toValue: Double,
  id: Int = 0
)

trait RawDataTable {
  val driver: slick.driver.JdbcDriver
  import driver.api._

  class RawDatas(tag: Tag) extends Table[RawData](tag, "rawdata") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def dataType = column[AggregationType]("dataType")
    def name = column[String]("name")
    def fromTime = column[DbTime]("fromTime")
    def toTime = column[DbTime]("toTime")
    def fromValue = column[Double]("fromValue")
    def toValue = column[Double]("toValue")
    def * = (dataType, name, fromTime, toTime, fromValue, toValue, id) <> (RawData.tupled, RawData.unapply)
  }

  val rawDatas = TableQuery[RawDatas]

}
