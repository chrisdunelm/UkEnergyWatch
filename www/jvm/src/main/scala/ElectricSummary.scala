package org.ukenergywatch.www

import scalatags.Text.all._

case class Updateable()
object Updateable {

  class Builder[A](data: A) {
    var nextId = 0
    //var 

    private def makeId(): String = {
      val ret = s"_$nextId"
      nextId += 1
      ret
    }

    def apply(valueFn: A => Tag): Tag = {
      val updateElementId = makeId()
      valueFn(data)(id := updateElementId)
    }

  }

  def builder[A](loadFn: () => A): Builder[A] = new Builder(loadFn())
}

trait ElectricSummary {

  object electricSummary {
/*
    def loadData(): Data = {
      Data(util.Random.nextInt())
    }
 */
    def view(): String = {
      "Not done yet"
      /*val upd = Updateable.builder(loadData)
      "<!DOCTYPE html>" + html(
        head(
          script(src := "/js/www-fastopt.js")
        ),
        body(
          h1("Electric Summary"),
          div(
            "My number is: ", upd(data => span(data.aNumber)), "."
          )
        )
      )*/
    }

  }

}
