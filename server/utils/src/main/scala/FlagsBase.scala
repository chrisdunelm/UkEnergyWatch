package org.ukenergywatch.utils

import scala.reflect.runtime.universe._
import scala.language.implicitConversions
import scala.annotation.tailrec

class FlagsException(msg: String) extends Exception(msg)

object FlagsBase {

  class Flag[T : TypeTag](
    val name: String,
    val shortName: Option[String],
    val defaultValue: Option[T],
    val ldescription: Option[String]
  ) {

    private[FlagsBase] var value: Option[T] = defaultValue

    private[FlagsBase] def set(v: String): Unit = {
      typeOf[T] match {
        case t if t =:= typeOf[String] => value = Some(v.asInstanceOf[T])
        case t if t =:= typeOf[Int] => value = Some(v.toInt.asInstanceOf[T])
        case t if t =:= typeOf[Double] => value = Some(v.toDouble.asInstanceOf[T])
        case _ => throw new Exception(s"Unsupported flag type: ${typeOf[T]}")
      }
    }

    def apply(): T = value.get

  }

  // Not using Option directly to avoid implicit methods to Option[T]
  case class OptionalValue[T](value: Option[T])
  object OptionalValue {
    def none[T] = OptionalValue[T](None)
  }

  private val longNameWithValueRx = """--(\w+)=(.+)""".r
  private val longNameOnlyRx = """--(\w+)""".r

  implicit def toOptionalValue[T](value: T): OptionalValue[T] = OptionalValue(Some(value))

}

trait FlagsBase {
  import FlagsBase._

  private var allFlags: Seq[Flag[_]] = Seq.empty

  @tailrec final def parse(args: Seq[String]): Unit = {
    args match {
      case Seq(longNameWithValueRx(longName, value), tail @ _*) =>
        allFlags.find(_.name == longName).get.set(value)
        parse(tail)
      case Seq(longNameOnlyRx(longName), value, tail @ _*) =>
        allFlags.find(_.name == longName).get.set(value)
        parse(tail)
      case Seq() =>
        // All done, check all non-default flags are initialised
        val uninitialisedFlags = allFlags.filter(_.value.isEmpty)
        if (uninitialisedFlags.nonEmpty) {
          val flagList = uninitialisedFlags.map(_.name).mkString(", ")
          throw new FlagsException(s"Uninitialised flags: $flagList")
        }
      case _ =>
        throw new Exception(s"Cannot understand args part: '$args'")
    }
  }

  protected def flag[T : TypeTag](
    name: String,
    shortName: OptionalValue[String] = OptionalValue.none[String],
    defaultValue: OptionalValue[T] = OptionalValue.none[T],
    description: OptionalValue[String] = OptionalValue.none[String]
  ): Flag[T] = {
    val flag = new Flag[T](name, shortName.value, defaultValue.value, description.value)
    allFlags = allFlags :+ flag
    flag
  }

  protected def register(other: FlagsBase): Unit = {
    allFlags ++= other.allFlags
  }

}
