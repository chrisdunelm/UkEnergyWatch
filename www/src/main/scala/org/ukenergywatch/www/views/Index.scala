package org.ukenergywatch.www.views

import scalatags._
import scalatags.all._

import org.ukenergywatch.www.Database
//import org.ukenergywatch.db.DalComp
import org.joda.time.DateTime
import org.ukenergywatch.wwwcommon._

case class Layout(title: String, content: Node)

object Layout {

  def view(data: Layout): Node = {
    html(
      head(
        Tags2.title(s"UK Energy Watch - ${data.title}"),
        script(src := "http://d3js.org/d3.v3.min.js"),
        script(src := "/js/wwwjs-preopt.js")
      ),
      body(
        h1("UK Energy Watch"),
        data.content
      )
    )
  }

}

case class Index(
  indexUpdate: IndexUpdate
)

object Index {

  import Database.dal.profile.simple.Session

  def render(): Node = {
    Database.dal.database.withSession { implicit session =>
      val indexUpdate = getUpdateInternal()
      val indexData = Index(indexUpdate)
      view(indexData)
    }
  }

  private def view(indexData: Index): Node = {
    val frag =
      div(
        p(id := "a", "Hello world!"),
        div(id := "freq",
          IndexUpdate.htmlGridFrequency(indexData.indexUpdate)
        ),
        script("Index().main()")
      )
    Layout.view(Layout("Home", frag))
  }

  def getUpdate(): IndexUpdate = {
    Database.dal.database.withSession { implicit session =>
      getUpdateInternal()
    }
  }

  private def getUpdateInternal()(implicit session: Session): IndexUpdate = {
    val dal = Database.dal
    val freq = dal.getLatestGridFrequency()
    IndexUpdate(freq.get.frequency + util.Random.nextDouble() * 10.0, freq.get.endTime.toString)
  }

}