package org.ukenergywatch.www

import org.scalajs.dom

trait ModelUpdater[Data] extends UpdateableModel[Data] with DomBundleSpec {

  def update(data: Data): Unit = {
    fragments(data).foreach { case (id, tag) =>
      val el = dom.document.getElementById(id.value)
      el.parentNode.replaceChild(tag.render, el)
    }
  }

}
