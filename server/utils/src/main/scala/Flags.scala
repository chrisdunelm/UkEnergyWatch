package org.ukenergywatch.utils

import scala.reflect.runtime.universe._
import scala.language.implicitConversions
import scala.annotation.tailrec

class FlagsException(msg: String) extends Exception(msg)

trait FlagsComponent {

  object FlagsBase {

    class Flag[T : TypeTag](
      val name: String,
      val shortName: Option[String],
      val defaultValue: Option[T],
      val description: Option[String]
    ) {
      private[FlagsComponent] var flagValue: Option[T] = defaultValue
      private[FlagsComponent] def isBoolean: Boolean = typeOf[T] =:= typeOf[Boolean]
      private def booleanValue(v: String): Boolean = v.toLowerCase match {
        case "true" => true
        case "false" => false
        case _ => throw new FlagsException(s"Unrecognised boolean value: '$v'")
      }
      private[FlagsComponent] def set(v: String): Unit = {
        typeOf[T] match {
          case t if t =:= typeOf[String] => flagValue = Some(v.asInstanceOf[T])
          case t if t =:= typeOf[Int] => flagValue = Some(v.toInt.asInstanceOf[T])
          case t if t =:= typeOf[Double] => flagValue = Some(v.toDouble.asInstanceOf[T])
          case t if t =:= typeOf[Boolean] => flagValue = Some(booleanValue(v).asInstanceOf[T])
          case _ => throw new FlagsException(s"Unsupported flag type: ${typeOf[T]}")
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

    private[FlagsComponent] var allFlags: Seq[Flag[_]] = Seq.empty

    private val longNameWithValueRx = """--(\w+)=(.+)""".r
    private val longNameOnlyRx = """--(\w+)""".r

    def parse(args: Seq[String], allowUnknownFlags: Boolean = false): Unit = {
      @tailrec def loop(args: List[String]): Unit = {
        def find(errorName: String)(pred: Flag[_] => Boolean): Flag[_] = {
          allFlags.find(pred) match {
            case Some(flag) => flag
            case None =>
              if (allowUnknownFlags) {
                new Flag[String]("", None, None, None)
              } else {
                throw new FlagsException(s"Passed flag not found: '$errorName'")
              }
          }
        }
        args match {
          case longNameWithValueRx(longName, value) :: tail =>
            find(longName)(_.name == longName).set(value)
            loop(tail)
          case longNameOnlyRx(longName) :: value :: tail =>
            val flag = find(longName)(_.name == longName)
            if (flag.isBoolean) {
              if (value.startsWith("-")) {
                flag.set("true")
                loop(value :: tail)
              } else {
                flag.set(value)
                loop(tail)
              }
            } else {
              flag.set(value)
              loop(tail)
            }
          case longNameOnlyRx(longName) :: Nil =>
            val flag = find(longName)(_.name == longName)
            if (flag.isBoolean || allowUnknownFlags) {
              flag.set("true")
              loop(Nil)
            } else {
              println(flag.name)
              throw new FlagsException(s"Cannot understand args part: '$args'")
            }
          case Nil =>
            // All done, check all non-default flags are initialised
            val uninitialisedFlags = allFlags.filter(_.flagValue.isEmpty)
            if (uninitialisedFlags.nonEmpty) {
              val flagList = uninitialisedFlags.map(_.name).mkString(", ")
              throw new FlagsException(s"Uninitialised flags: $flagList")
            }
          case x :: tail =>
            if (allowUnknownFlags) {
              loop(tail)
            } else {
              throw new FlagsException(s"Cannot understand args part: '$args'")
            }
        }
      }
      loop(args.toList)
    }

    def parse(args: String): Unit = parse(args.split(' '))

  }

}
