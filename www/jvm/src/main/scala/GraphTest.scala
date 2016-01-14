package org.ukenergywatch.www

import scalatags.Text.all._

trait GraphTest {

  object graphTest {

    def view(): String = {
      "<!DOCTYPE html>" + html(
        head(
          title := "Graph Test",
          link(href := "/css/reset.css", rel := "stylesheet", `type` := "text/css"),
          script(src := "/js/www-fastopt.js")
        ),
        body(
          h1("GraphTest"),
          div(id := "graphs", "here we are")
        ),
        script("org.ukenergywatch.www.Graph().init('graphs');")
      )
    }

  }

}
