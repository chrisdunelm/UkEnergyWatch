package org.ukenergywatch.www

trait TextBundleSpec extends BundleSpec {
  type BundleBuilder = scalatags.text.Builder
  type BundleOutput = String
  type BundleFrag = String
  val bundle: BundleType = scalatags.Text
}
