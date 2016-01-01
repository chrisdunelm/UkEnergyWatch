package org.ukenergywatch.oldfueltype

import org.ukenergywatch.utils._
import org.ukenergywatch.utils.JavaTimeExtensions._
import org.ukenergywatch.utils.StringExtensions._
import org.ukenergywatch.utils.units._
import slick.driver.{ JdbcDriver, H2Driver, MySQLDriver }
import slick.dbio._
import java.time.LocalDateTime
import scala.concurrent.{ Future, Await }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success

/*
mysql> show tables;
+-------------------------+
| Tables_in_ukenergywatch |
+-------------------------+
| bmunitsfpn              |
| frequencies             |
| gasinstantaneousflow    |
| generationbyfueltypes   |
| logins                  |
| logrobots               |
| logsessions             |
| logurls                 |
| powerstationcategories  |
| powerstationcompanies   |
| powerstations           |
| regbmunits              |
| soladin600              |
+-------------------------+
13 rows in set (0.00 sec)

mysql> describe generationbyfueltypes;
+------------------+---------------------------------------------------------------------------------------------------+------+-----+---------+----------------+
| Field            | Type                                                                                              | Null | Key | Default | Extra          |
+------------------+---------------------------------------------------------------------------------------------------+------+-----+---------+----------------+
| Id               | int(11)                                                                                           | NO   | PRI | NULL    | auto_increment |
| When             | datetime                                                                                          | NO   | MUL | NULL    |                |
| SettlementDate   | date                                                                                              | NO   |     | NULL    |                |
| SettlementPeriod | tinyint(4)                                                                                        | NO   |     | NULL    |                |
| Type             | enum('ccgt','ocgt','oil','coal','nuclear','wind','ps','npshyd','other','intfr','intirl','intned') | NO   |     | NULL    |                |
| Interconnect     | tinyint(1)                                                                                        | NO   |     | NULL    |                |
| Value            | mediumint(9)                                                                                      | NO   |     | NULL    |                |
+------------------+---------------------------------------------------------------------------------------------------+------+-----+---------+----------------+
7 rows in set (0.00 sec)

mysql> select * from generationbyfueltypes order by id desc limit 15;
+---------+---------------------+----------------+------------------+---------+--------------+-------+
| Id      | When                | SettlementDate | SettlementPeriod | Type    | Interconnect | Value |
+---------+---------------------+----------------+------------------+---------+--------------+-------+
| 9623580 | 2015-12-02 12:25:00 | 2015-12-02     |               25 | intned  |            1 |   838 |
| 9623579 | 2015-12-02 12:25:00 | 2015-12-02     |               25 | intirl  |            1 |     0 |
| 9623578 | 2015-12-02 12:25:00 | 2015-12-02     |               25 | intfr   |            1 |  1002 |
| 9623577 | 2015-12-02 12:25:00 | 2015-12-02     |               25 | other   |            0 |  2069 |
| 9623576 | 2015-12-02 12:25:00 | 2015-12-02     |               25 | npshyd  |            0 |   675 |
| 9623575 | 2015-12-02 12:25:00 | 2015-12-02     |               25 | ps      |            0 |   787 |
| 9623574 | 2015-12-02 12:25:00 | 2015-12-02     |               25 | wind    |            0 |  4738 |
| 9623573 | 2015-12-02 12:25:00 | 2015-12-02     |               25 | nuclear |            0 |  8236 |
| 9623572 | 2015-12-02 12:25:00 | 2015-12-02     |               25 | coal    |            0 | 10205 |
| 9623571 | 2015-12-02 12:25:00 | 2015-12-02     |               25 | oil     |            0 |     0 |
| 9623570 | 2015-12-02 12:25:00 | 2015-12-02     |               25 | ocgt    |            0 |     0 |
| 9623569 | 2015-12-02 12:25:00 | 2015-12-02     |               25 | ccgt    |            0 | 10960 |
| 9623568 | 2015-12-02 12:20:00 | 2015-12-02     |               25 | intned  |            1 |   840 |
| 9623567 | 2015-12-02 12:20:00 | 2015-12-02     |               25 | intirl  |            1 |     0 |
| 9623566 | 2015-12-02 12:20:00 | 2015-12-02     |               25 | intfr   |            1 |  1002 |
+---------+---------------------+----------------+------------------+---------+--------------+-------+
 */

case class GenerationByFuelType(
  when: String,
  settlementDate: String,
  settlementPeriod: Int,
  typ: String,
  interconnect: Int,
  value: Int,
  id: Int = 0
)

trait GenerationByFuelTypesTable {
  val driver: slick.driver.JdbcDriver
  import driver.api._

  class GenerationByFuelTypes(tag: Tag) extends Table[GenerationByFuelType](tag, "generationbyfueltypes") {
    def id = column[Int]("Id", O.PrimaryKey, O.AutoInc)
    def when = column[String]("When")
    def settlementDate = column[String]("SettlementDate")
    def settlementPeriod = column[Int]("SettlementPeriod")
    def typ = column[String]("Type")
    def interconnect = column[Int]("Interconnect")
    def value = column[Int]("Value")
    def * = (when, settlementDate, settlementPeriod, typ, interconnect, value, id) <> (
      GenerationByFuelType.tupled, GenerationByFuelType.unapply)
  }

  object generationByFuelTypes extends TableQuery[GenerationByFuelTypes](new GenerationByFuelTypes(_))
}

trait DbParamsComponent {
  def dbParams: DbParams
  trait DbParams {
    def host: String
    def database: String
    def user: String
    def password: String
  }
}

trait DbComponent {
  val db: Db
  trait Db extends GenerationByFuelTypesTable {
    val driver: JdbcDriver
    def db: driver.api.Database
  }
}

trait DbMemoryComponent extends DbComponent {
  lazy val db = new DbMemory
  class DbMemory extends Db {
    lazy val driver = H2Driver
    lazy val db = driver.api.Database.forURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", driver="org.h2.Driver")
  }
}

trait DbMysqlComponent extends DbComponent {
  this: DbParamsComponent =>
  lazy val db = new DbMysql
  class DbMysql extends Db {
    lazy val driver = MySQLDriver
    lazy val db = driver.api.Database.forURL(
      s"jdbc:mysql://${dbParams.host}:3306/${dbParams.database}",
      driver = "com.mysql.jdbc.Driver",
      user = dbParams.user,
      password = dbParams.password
    )
  }
}

trait OldFuelTypeComponent {
  this: SchedulerComponent
      with DownloaderComponent
      with DbComponent
      with ClockComponent =>

  import db.driver.api._

  def init(elexonKey: String, instantDownload: Boolean, runForever: Boolean): Unit = {

    def onTimer(n: Int): ReAction = {
      //println("fetching data from elexonportal...")
      val url = s"https://downloads.elexonportal.co.uk/fuel/download/latest?key=$elexonKey"
      val download = downloader.get(url)
      Await.ready(download, 30.seconds.toConcurrent)
      download.value match {
        case Some(Success(bytes)) =>
          //println("Got data successfully")
          handleDownload(bytes.toStringUtf8)
        case _ =>
          println("ERROR downloading")
      }
      ReAction.Success
    }

    if (instantDownload) {
      onTimer(0)
    } else {
      scheduler.run(5.minutes, 2.minutes)(onTimer)
      if (runForever) {
        Thread.sleep(1000L * 60L * 60L * 24L * 365L * 100L) // 100 years!
      }
    }
  }

  def handleDownload(s: String): Unit = {
    import scala.xml.XML

    val xml = XML.loadString(s)
    val inst = xml \ "INST"
    val instAt = inst.head.attribute("AT").get.head.text
    val instRx = """(\d{4})-(\d{2})-(\d{2}) (\d{2}):(\d{2}):(\d{2})""".r
    val instDt = instAt match {
      case instRx(year, month, day, hour, minute, second) =>
        LocalDateTime.of(year.toInt, month.toInt, day.toInt, hour.toInt, minute.toInt, second.toInt)
    }
    //println(instAt)
    val settlementDate = instAt.take(10)
    val settlementPeriod = instDt.getHour * 2 + instDt.getMinute / 30 + 1
    val fuels = inst \ "FUEL"
    val gensByType: Map[String, GenerationByFuelType] = (for {
      fuel <- fuels
    } yield {
      //println(fuel)
      val typ = fuel.attribute("TYPE").get.head.text.toLowerCase
      val value = fuel.attribute("VAL").get.head.text.toInt
      val interconnect = fuel.attribute("IC").get.head.text match {
        case "N" => 0
        case "Y" => 1
      }
      typ -> GenerationByFuelType(instAt, settlementDate, settlementPeriod, typ, interconnect, value)
    }).toMap
    val okTypes = Set("ccgt","ocgt","oil","coal","nuclear","wind","ps","npshyd","other","intfr","intirl","intned")
    val otherAdd = gensByType.values.filterNot(x => okTypes(x.typ)).foldLeft(0)(_ + _.value)
    val genOther0 = gensByType("other")
    val toInsert = gensByType.values
      .filter(x => okTypes(x.typ))
      .filter(_.typ != "other")
      .toList :+ genOther0.copy(value = genOther0.value + otherAdd)

    // check that data is not already in database, if not put new data in to database
    val actions: DBIO[_] = {
      val dataExists: DBIO[Boolean] = db.generationByFuelTypes.filter(_.when === instAt).exists.result
      dataExists.flatMap { dataExists =>
        if (dataExists) {
          // Do nothing else
          println("Data already in database, not re-inserting")
          DBIO.successful(())
        } else {
          // Insert into database
          //println(s"Inserting ${toInsert.size} rows into database")
          db.generationByFuelTypes ++= toInsert
        }
      }
    }
    val insertFuture = db.db.run(actions.transactionally)
    Await.result(insertFuture, 30.seconds.toConcurrent)
  }

}

object OldFuelType {

  val elexonData20151207184000 = """<?xml version="1.0"?>
<GENERATION_BY_FUEL_TYPE_TABLE>
<INST AT="2015-12-07 18:40:00" TOTAL="45104"><FUEL TYPE="CCGT" IC="N" VAL="14992" PCT="33.2"></FUEL><FUEL TYPE="OCGT" IC="N" VAL="0" PCT="0.0"></FUEL><FUEL TYPE="OIL" IC="N" VAL="0" PCT="0.0"></FUEL><FUEL TYPE="COAL" IC="N" VAL="10322" PCT="22.9"></FUEL><FUEL TYPE="NUCLEAR" IC="N" VAL="8767" PCT="19.4"></FUEL><FUEL TYPE="WIND" IC="N" VAL="5531" PCT="12.3"></FUEL><FUEL TYPE="PS" IC="N" VAL="331" PCT="0.7"></FUEL><FUEL TYPE="NPSHYD" IC="N" VAL="842" PCT="1.9"></FUEL><FUEL TYPE="OTHER" IC="N" VAL="2092" PCT="4.6"></FUEL><FUEL TYPE="INTFR" IC="Y" VAL="1246" PCT="2.8"></FUEL><FUEL TYPE="INTIRL" IC="Y" VAL="0" PCT="0.0"></FUEL><FUEL TYPE="INTNED" IC="Y" VAL="980" PCT="2.2"></FUEL><FUEL TYPE="INTEW" IC="Y" VAL="1" PCT="0.0"></FUEL></INST><HH SD="2015-12-07" SP="37" AT="18:00-18:30" TOTAL="45621"><FUEL TYPE="CCGT" IC="N" VAL="15425" PCT="33.8"></FUEL><FUEL TYPE="OCGT" IC="N" VAL="0" PCT="0.0"></FUEL><FUEL TYPE="OIL" IC="N" VAL="0" PCT="0.0"></FUEL><FUEL TYPE="COAL" IC="N" VAL="10479" PCT="23.0"></FUEL><FUEL TYPE="NUCLEAR" IC="N" VAL="8772" PCT="19.2"></FUEL><FUEL TYPE="WIND" IC="N" VAL="5436" PCT="11.9"></FUEL><FUEL TYPE="PS" IC="N" VAL="356" PCT="0.8"></FUEL><FUEL TYPE="NPSHYD" IC="N" VAL="836" PCT="1.8"></FUEL><FUEL TYPE="OTHER" IC="N" VAL="2093" PCT="4.6"></FUEL><FUEL TYPE="INTFR" IC="Y" VAL="1246" PCT="2.7"></FUEL><FUEL TYPE="INTIRL" IC="Y" VAL="0" PCT="0.0"></FUEL><FUEL TYPE="INTNED" IC="Y" VAL="978" PCT="2.1"></FUEL><FUEL TYPE="INTEW" IC="Y" VAL="0" PCT="0.0"></FUEL></HH><LAST24H FROM_SD="2015-12-06" FROM_SP="38" AT="18:30-18:30" TOTAL="862304"><FUEL TYPE="CCGT" IC="N" VAL="244031" PCT="28.3"></FUEL><FUEL TYPE="OCGT" IC="N" VAL="33" PCT="0.0"></FUEL><FUEL TYPE="OIL" IC="N" VAL="0" PCT="0.0"></FUEL><FUEL TYPE="COAL" IC="N" VAL="200245" PCT="23.2"></FUEL><FUEL TYPE="NUCLEAR" IC="N" VAL="209982" PCT="24.4"></FUEL><FUEL TYPE="WIND" IC="N" VAL="77891" PCT="9.0"></FUEL><FUEL TYPE="PS" IC="N" VAL="7512" PCT="0.9"></FUEL><FUEL TYPE="NPSHYD" IC="N" VAL="21825" PCT="2.5"></FUEL><FUEL TYPE="OTHER" IC="N" VAL="49786" PCT="5.8"></FUEL><FUEL TYPE="INTFR" IC="Y" VAL="26440" PCT="3.1"></FUEL><FUEL TYPE="INTIRL" IC="Y" VAL="2066" PCT="0.2"></FUEL><FUEL TYPE="INTNED" IC="Y" VAL="19993" PCT="2.3"></FUEL><FUEL TYPE="INTEW" IC="Y" VAL="2498" PCT="0.3"></FUEL></LAST24H><LAST_UPDATED AT="2015-12-07 18:40:00"></LAST_UPDATED></GENERATION_BY_FUEL_TYPE_TABLE>"""

  def main(args: Array[String]): Unit = {
    println("Old fuel type")

    val elexonKey = args(0)
    val dbHost = args(1)
    val dbUser = args(2)
    val dbPassword = args(3)
    val instantDownload = args.length >= 5 && args(4) == "instant"

    trait InlineDbParamsComponent extends DbParamsComponent {
      val dbParams = InlineDbParams
      // work-round a compiler bug
      def dbHost1 = dbHost
      def dbUser1 = dbUser
      def dbPassword1 = dbPassword
      object InlineDbParams extends DbParams {
        def host = dbHost1
        def database = "ukenergywatch"
        def user = dbUser1
        def password = dbPassword1
      }
    }

    object App extends OldFuelTypeComponent
        with DbMysqlComponent
        with InlineDbParamsComponent
        with SchedulerRealtimeComponent
        //with DownloaderFakeComponent
        with DownloaderRealComponent
        with ClockRealtimeComponent
        with LogMemoryComponent

    //App.downloader.content = Map(
    //  s"https://downloads.elexonportal.co.uk/fuel/download/latest?key=$elexonKey" ->
    //    elexonData20151207184000.toBytesUtf8
    //)

    App.init(elexonKey, instantDownload, true)
  }

}
