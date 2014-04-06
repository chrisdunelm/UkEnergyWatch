/*package org.ukenergywatch.utils

import org.scalatest._
import org.ukenergywatch.utils_macros._

class SealedIteratorSpec extends FlatSpec with Matchers {

  "SealedIterator" should "Return None for anything that is not a sealed trait/class" in {
    trait NotSealed
    SealedIterator.values[Int] shouldBe None
    SealedIterator.values[String] shouldBe None
    SealedIterator.values[NotSealed] shouldBe None
    SealedIterator.values[Unit] shouldBe None
  }

  it should "Return a set of all derived objects of a sealed trait" in {
    sealed trait Sealed
    case object A extends Sealed
    case object B extends Sealed
    case object C extends Sealed
    SealedIterator.values[Sealed] shouldBe Some(Set(A, B, C))
  }

  it should "Return a set of all derived object of an abstract class" in {
    sealed abstract class Sealed
    object A extends Sealed
    object B extends Sealed
    SealedIterator.values[Sealed] shouldBe Some(Set(A, B))
  }

  it should "Work with type parameter" in {
    sealed trait Sealed
    object A extends Sealed
    def z[T]: Option[Set[T]] = SealedIterator.values[T]
    z[Sealed] shouldBe Some(Set(A))
  }

}
*/