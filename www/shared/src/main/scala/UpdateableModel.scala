package org.ukenergywatch.www

trait UpdateableModel[Data] extends BundleSpec {
  import bundle.all._
  protected def fragmentDescs: Seq[(Id, Data => Tag)]

  def fragments(data: Data): Map[Id, Tag] = fragmentDescs.map { case (id0, fragFn) =>
    val fragment: Tag = fragFn(data)(id := id0.value)
    id0 -> fragment
  }.toMap

}
