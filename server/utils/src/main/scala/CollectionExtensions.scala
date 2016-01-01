package org.ukenergywatch.utils

object CollectionExtensions {

  implicit class RichSeq[T](val s: Seq[T]) extends AnyVal {
    def only: T = s match {
      case Seq(a) => a
      case _ => throw new Exception("Not just 1 element in seq")
    }
  }

}
