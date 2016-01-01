package org.ukenergywatch.utils

import scala.reflect.runtime.universe._
import scala.language.implicitConversions
import scala.annotation.tailrec

class FlagsException(msg: String) extends Exception(msg)

trait FlagsBaseComponent {

  object FlagsBase {

    class Flag[T : TypeTag](
      val name: String,
      val shortName: Option[String],
      val defaultValue: Option[T],
      val ldescription: Option[String]
    ) {
      private[FlagsBaseComponent] var flagValue: Option[T] = defaultValue
      private[FlagsBaseComponent] def set(v: String): Unit = {
        typeOf[T] match {
          case t if t =:= typeOf[String] => flagValue = Some(v.asInstanceOf[T])
          case t if t =:= typeOf[Int] => flagValue = Some(v.toInt.asInstanceOf[T])
          case t if t =:= typeOf[Double] => flagValue = Some(v.toDouble.asInstanceOf[T])
          case _ => throw new Exception(s"Unsupported flag type: ${typeOf[T]}")
        }
      }
      def apply(): T = flagValue.get
    }

    // Not using Option directly to avoid implicit methods to Option[T]
    case class OptionalValue[T](value: Option[T])
    object OptionalValue {
      def none[T] = OptionalValue[T](None)
    }

    implicit def toOptionalValue[T](value: T): OptionalValue[T] = OptionalValue(Some(value))

  }

  trait FlagsBase {
    import FlagsBase._
    protected def flag[T : TypeTag](
      name: String,
      shortName: OptionalValue[String] = OptionalValue.none[String],
      defaultValue: OptionalValue[T] = OptionalValue.none[T],
      description: OptionalValue[String] = OptionalValue.none[String]
    ): Flag[T] = {
      val flag = new Flag[T](name, shortName.value, defaultValue.value, description.value)
      flags.allFlags = flags.allFlags :+ flag
      flag
    }
  }

  object flags {
    import FlagsBase._

    private[FlagsBaseComponent] var allFlags: Seq[Flag[_]] = Seq.empty

    private val longNameWithValueRx = """--(\w+)=(.+)""".r
    private val longNameOnlyRx = """--(\w+)""".r

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
          val uninitialisedFlags = allFlags.filter(_.flagValue.isEmpty)
          if (uninitialisedFlags.nonEmpty) {
            val flagList = uninitialisedFlags.map(_.name).mkString(", ")
            throw new FlagsException(s"Uninitialised flags: $flagList")
          }
        case _ =>
          throw new Exception(s"Cannot understand args part: '$args'")
      }
    }

  }

}
