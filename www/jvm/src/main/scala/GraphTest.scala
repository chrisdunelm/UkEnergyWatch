package org.ukenergywatch.www

import scalatags.Text.all._

trait GraphTest {

  object graphTest {

    def view(): String = {
      "<!DOCTYPE html>" + html(
        head(
          script(src := "/js/www-fastopt.js")
        ),
        body(
          h1("GraphTest"),
          div(id := "graphs")
        ),
        script("org.ukenergywatch.www.Graph().init('graphs');")
      )
    }

  }

}
