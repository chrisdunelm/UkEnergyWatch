package org.ukenergywatch.www

import scalatags.generic.{ Bundle, TypedTag }

trait BundleSpec {
  type BundleBuilder
  type BundleOutput <: BundleFrag
  type BundleFrag
  type BundleType = Bundle[BundleBuilder, BundleOutput, BundleFrag]
  type Tag = TypedTag[BundleBuilder, BundleOutput, BundleFrag]
  val bundle: BundleType
}
