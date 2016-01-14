package org.ukenergywatch.www

import org.scalajs.dom

trait DomBundleSpec extends BundleSpec {
  type BundleBuilder = dom.Element
  type BundleOutput = dom.Element
  type BundleFrag = dom.Node
  val bundle: BundleType = scalatags.JsDom
}
