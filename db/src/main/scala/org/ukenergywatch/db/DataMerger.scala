package org.ukenergywatch.db

import scala.slick.driver.JdbcProfile

trait DataMerger {
  this: GridFrequencyTable with GenByFuelTable =>
  val profile: JdbcProfile

  import profile.simple._

  def getLatestGridFrequency()(implicit session: Session): Option[GridFrequency] = {
    val liveLatest = GridFrequenciesLive.sortBy(_.endTime.desc).firstOption
    val latest = GridFrequencies.sortBy(_.endTime).firstOption
    (liveLatest, latest) match {
      case (Some(a), Some(b)) => Some(Seq(a, b).sortBy(-_.endTime).head)
      case (Some(a), None) => Some(a)
      case (None, Some(a)) => Some(a)
      case _ => None
    }
  }

  def getLatestGenByFuel()(implicit session: Session): Option[Seq[GenByFuel]] = {
    val liveLatest = GenByFuelsLive.sortBy(_.toTime.desc).firstOption
    val latest = GenByFuels.sortBy(_.toTime.desc).firstOption
    val data = (liveLatest, latest) match {
      case (Some(a), Some(b)) => Some(if (a.toTime > b.toTime) {
        GenByFuelsLive.filter(_.toTime === a.toTime).list
      } else {
        GenByFuels.filter(_.toTime === b.toTime).list
      })
      case (Some(a), None) => Some(GenByFuelsLive.filter(_.toTime === a.toTime).list)
      case (None, Some(a)) => Some(GenByFuels.filter(_.toTime === a.toTime).list)
      case _ => None
    }
    data.map { data =>
      data.map { elem =>
        if (elem.mw < 0) {
          elem.copy(mw = 0)
        } else {
          elem
        }
      }
    }
  }

}
