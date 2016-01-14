package org.ukenergywatch.www

trait ElectricSummary {

  object electricSummary {
    import ElectricSummaryModel._

    object Model extends ElectricSummaryModel with TextBundleSpec

    import scalatags.Text.all._

    def loadData(): Data = {
      Data(-1, "theword")
    }

    def view(): String = {
      val data = loadData()
      val frags = Model.fragments(data)
      "<!DOCTYPE html>" + html(
        head(
          script(src := "/js/www-fastopt.js")
        ),
        body(
          h1("Electric Summary"),
          div(
            "My number is: ", frags(aNumber), "."
          ),
          div(
            "My word is: '", frags(aWord), "'."
          )
        ),
        script(
          "org.ukenergywatch.www.ElectricSummary().test();"
        )
      )
    }

  }

}
