package org.ukenergywatch.db

import scala.slick.driver.JdbcProfile
import org.joda.time.ReadableInstant
import org.joda.time.ReadableInterval
import org.joda.time.Instant
import org.joda.time.Interval
import org.ukenergywatch.utils.JodaTimeExtensions._


trait Mergeable[T] {
  def fromTime: Int
  def toTime: Int
  protected def fromValue: T
  protected def toValue: T

  def doesOverlap(other: Mergeable[_]): Boolean =
    this.toTime > other.fromTime && other.toTime > this.fromTime

  def canMerge(other: Mergeable[T]): Boolean =
    this.fromValue == this.toValue && this.fromValue == other.fromValue && this.fromValue == other.toValue

}

trait Merger[T <: Mergeable[U], U] {
  protected def merge(cur: Seq[T], item: T, insert: T => Unit, updateFrom: (T, Int) => Unit, updateTo: (T, Int) => Unit, delete: T => Unit) {
    cur match {
      case list if list.length > 2 =>
        throw new Exception
      case list if list.exists(x => item.doesOverlap(x)) =>
        throw new Exception
      case List(a, b) if a.toTime == item.fromTime && b.fromTime == item.toTime && item.canMerge(a) && item.canMerge(b) =>
        // new item is between two existing items, and is mergeable
        updateTo(a, b.toTime)
        delete(b)
      case List(a, _*) if a.toTime == item.fromTime && item.canMerge(a) =>
        // new item can merge with previous existing item
        updateTo(a, item.toTime)
      case List(a) if item.toTime == a.fromTime && item.canMerge(a) =>
        // new item can merge with following existing item
        updateFrom(a, item.fromTime)
      case List(_, a) if item.toTime == a.fromTime && item.canMerge(a) =>
        // new item can merge with following existing item
        updateFrom(a, item.fromTime)
      case _ =>
        // Cannot merge with anything, insert new item
        insert(item)
    }
  }
}

trait MergeableTable {
  val profile: JdbcProfile

  import profile.simple._

  trait IntTimeRange {
    def fromTime: Column[Int]
    def toTime: Column[Int]

    def from: ReadableInstant = ???
    def to: ReadableInstant = ???
  }

  abstract class TimeMergeTable[T](tag: Tag, name: String) extends Table[T](tag, name) with IntTimeRange {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def fromTime = column[Int]("fromTime")
    def toTime = column[Int]("toTime")
  }

}

trait DownloadTable extends MergeableTable {
  val profile: JdbcProfile

  import profile.simple._

  case class Download(id: Int, downloadType: Int, fromTime: Int, toTime: Int) extends Mergeable[Unit] {
    protected def fromValue: Unit = Unit
    protected def toValue: Unit = Unit
  }

  class Downloads(tag: Tag) extends TimeMergeTable[Download](tag, "downloads") {
    def downloadType = column[Int]("downloadType")
    def * = (id, downloadType, fromTime, toTime) <> (Download.tupled, Download.unapply)
  }

  object Downloads extends TableQuery(new Downloads(_)) with Merger[Download, Unit] {

    val TYPE_BMUFPN = 1

    def mergeInsert(item: Download)(implicit session: Session) {
      val q = Downloads
        .filter(x => x.downloadType === item.downloadType && x.toTime >= item.fromTime && x.fromTime <= item.toTime)
        .sortBy(_.fromTime)
        //.take(3)
      val result = q.list.take(3)
      def insert(e: Download): Unit = Downloads += e
      def updateFrom(e: Download, from: Int): Unit = Downloads.filter(_.id === e.id).map(_.fromTime).update(from)
      def updateTo(e: Download, to: Int): Unit = Downloads.filter(_.id === e.id).map(_.toTime).update(to)
      def delete(e: Download): Unit = Downloads.filter(_.id === e.id).delete
      merge(result, item, insert, updateFrom, updateTo, delete)
    }

    def getLatest(downloadType: Int)(implicit session: Session): Option[ReadableInstant] = {
      Downloads.sortBy(_.toTime.desc).firstOption.map(x => new Instant(x.toTime * 1000L))
    }

    def getLastGap(downloadType: Int)(implicit session: Session): Option[ReadableInterval] = {
      val q = Downloads.sortBy(_.toTime.desc)//.take(2)
      val res = q.list.take(2).reverse
      res match {
        case List() => None
        case List(a) => Some(new Interval(0.toInstant, a.fromTime.toInstant))
        case List(a, b) => Some(new Interval(a.toTime.toInstant, b.fromTime.toInstant))
      }
    }

  }
  
}

trait BmUnitFpnsTable extends MergeableTable {
  val profile: JdbcProfile

  import profile.simple._

  case class BmUnitFpn(id: Int, bmUnitId: String, fromTime: Int, fromMw: Float, toTime: Int, toMw: Float) extends Mergeable[Float] {
    protected def fromValue: Float = fromMw
    protected def toValue: Float = toMw
  }

  class BmUnitFpns(tag: Tag) extends TimeMergeTable[BmUnitFpn](tag, "bmunitfpns") {
    def bmUnitId = column[String]("bmUnitId")
    def fromMw = column[Float]("fromMw")
    def toMw = column[Float]("toMw")
    def * = (id, bmUnitId, fromTime, fromMw, toTime, toMw) <> (BmUnitFpn.tupled, BmUnitFpn.unapply)
  }

  object BmUnitFpns extends TableQuery(new BmUnitFpns(_)) with Merger[BmUnitFpn, Float] {
    def mergeInsert(item: BmUnitFpn)(implicit session: Session) {
      val q = BmUnitFpns
        .filter(x => x.bmUnitId === item.bmUnitId && x.toTime >= item.fromTime && x.fromTime <= item.toTime)
        .sortBy(_.fromTime)
        //.take(3)
      val result = q.list.take(3)
      def insert(e: BmUnitFpn): Unit = BmUnitFpns += e
      def updateFrom(e: BmUnitFpn, from: Int): Unit = BmUnitFpns.filter(_.id === e.id).map(_.fromTime).update(from)
      def updateTo(e: BmUnitFpn, to: Int): Unit = BmUnitFpns.filter(_.id === e.id).map(_.toTime).update(to)
      def delete(e: BmUnitFpn): Unit = BmUnitFpns.filter(_.id === e.id).delete
      merge(result, item, insert, updateFrom, updateTo, delete)
    }
    def getSpot(bmUnitId: String, when: ReadableInstant)(implicit session: Session): Option[Double] = {
      val whenSeconds = (when.getMillis / 1000).toInt
      val q = BmUnitFpns.filter(x => x.bmUnitId === bmUnitId && x.fromTime <= whenSeconds && x.toTime > whenSeconds)
      val item = q.firstOption
      for (item <- item) yield {
        val itemRange = (item.toTime - item.fromTime).toDouble
        val tFraction = (whenSeconds.toDouble - item.fromTime.toDouble) / itemRange
        item.fromMw + (item.toMw - item.fromMw) * tFraction
      }
    }
    def getRange(bmUnitId: String, interval: ReadableInterval)(implicit session: Session): Seq[BmUnitFpn] = {
      val seconds0 = (interval.getStartMillis / 1000).toInt
      val seconds1 = (interval.getEndMillis / 1000).toInt
      val q = BmUnitFpns.filter(x => x.bmUnitId === bmUnitId && x.toTime > seconds0 && x.fromTime < seconds1).sortBy(_.fromTime)
      q.list
    }
  }

}

trait DalComp {
  val dal: Dal
  trait Dal
      extends DownloadTable
      with BmUnitFpnsTable {

    val profile: JdbcProfile
    import profile.simple._

    val database: Database

    def ddls: Seq[profile.DDL] = Seq(
      Downloads.ddl,
      BmUnitFpns.ddl
    )

  }
}