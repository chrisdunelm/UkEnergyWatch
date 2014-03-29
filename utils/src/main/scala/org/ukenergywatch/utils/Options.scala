package org.ukenergywatch.utils

import scala.reflect.ClassTag
import scala.reflect.macros.Context
import language.experimental.macros

object Options {

  private def opt[T: c.WeakTypeTag](c: Context)(default: Option[c.Expr[T]], spec: Option[c.Expr[OptionSpec]]): c.Expr[() => T] = {
    import c.universe._

    val valPosition = c.enclosingPosition
    // Determine name of val
    val names = c.enclosingClass match {
      case ModuleDef(_, _, Template(_, _, body)) =>
        body.collect {
          case ValDef(_, name, _, rhs) if rhs.pos == valPosition => name
        }
      case _ => Seq()
    }
    val name = names match {
      case Seq(name) => name
      case _ => c.abort(valPosition, "Cannot determine option name. Make sure it is an object within a sealed trait.")
    }
    // Determine if this is an enum or a normal flag
    val symbol = weakTypeOf[T].typeSymbol
    val (fnName, extraArgs) = if (symbol.isClass && symbol.asClass.isSealed) {
      // Enum flag - find all derived objects of the sealed trait/class
      val children = symbol.asClass.knownDirectSubclasses.toList
      if (children.isEmpty) {
        c.warning(c.enclosingPosition, s"No derived objects found in enum flag: '$symbol'")
      }
      val mapExpr = if (children.forall(_.isModuleClass)) {
        val mapApply = Select(reify(Map).tree, newTermName("apply"))
        val tuple2Apply = Select(reify(Tuple2).tree, newTermName("apply"))
        val mapApplied = Apply(mapApply, children.map(ch =>
          Apply(tuple2Apply, List(c.literal(ch.name.encoded).tree, Ident(ch.asClass.module)))
        ))
        c.Expr[Map[String, T]](mapApplied)
      } else {
        c.abort(c.enclosingPosition, "All children must be objects")
      }
      ("optEnum", List(mapExpr.tree))
    } else {
      // Normal flag
      ("optNormal", List())
    }
    val defaultExpr = default match {
      case Some(defaultExpr) => reify(Some(defaultExpr.splice))
      case None => reify(None.asInstanceOf[Option[T]])
    }
    val specExpr = spec match {
      case Some(expr) => reify(Some(expr.splice))
      case None => reify(None.asInstanceOf[Option[OptionSpec]])
    }
    val fn = Select(This(c.enclosingClass.symbol.asModule.moduleClass), newTermName(fnName))
    c.Expr[() => T](Apply(fn, List(c.literal(name.encoded).tree, defaultExpr.tree, specExpr.tree) ++ extraArgs))
  }

  def opt_impl[T: c.WeakTypeTag](c: Context)(): c.Expr[() => T] = opt[T](c)(None, None)
  def optDef_impl[T: c.WeakTypeTag](c: Context)(default: c.Expr[T]): c.Expr[() => T] = opt[T](c)(Some(default), None)
  def optSpec_impl[T: c.WeakTypeTag](c: Context)(spec: c.Expr[OptionSpec]): c.Expr[() => T] = opt[T](c)(None, Some(spec))
  def optDefSpec_impl[T: c.WeakTypeTag](c: Context)(default: c.Expr[T], spec: c.Expr[OptionSpec]): c.Expr[() => T] = opt[T](c)(Some(default), Some(spec))

  private case class FlagType(cls: Option[ClassTag[_]], enum: Option[Map[String, _]])
  private case class FlagInfo(mainName: String, flagType: FlagType, optionSpec: Option[OptionSpec])

}

case class OptionSpec(name: String = "", shortName: Char = '\0', help: String = "")

trait Options {
  import Options._

  private var registrations = List[Options]()
  private var required = List[Set[String]]()
  private var names = Map[String, FlagInfo]()
  private var values = Map[String, Any]()

  protected def register(opts: Options*): Unit = for (opt <- opts) {
    registrations = registrations :+ opt
    // Merge names
    for ((k, v) <- opt.names) {
      if (names.contains(k)) {
        throw new Exception(s"Duplicate option name: '$k'")
      }
      names = names + (k -> v)
    }
  }

  private def mergeValues(): Unit = {
    for (registration <- registrations) {
      registration.values = values
      registration.mergeValues()
    }
  }

  protected def opt[T](): () => T = macro Options.opt_impl[T]
  protected def opt[T](default: T): () => T = macro Options.optDef_impl[T]
  protected def opt[T](spec: OptionSpec): () => T = macro Options.optSpec_impl[T]
  protected def opt[T](default: T, spec: OptionSpec): () => T = macro Options.optDefSpec_impl[T]

  protected def optNormal[T : ClassTag](autoName: String, default: Option[T], spec: Option[OptionSpec]): () => T = {
    val cls = implicitly[ClassTag[T]]
    val flagType = FlagType(Some(cls), None)
    addFlag(autoName, default, spec, flagType)
  }

  protected def optEnum[T](autoName: String, default: Option[T], spec: Option[OptionSpec], clssByName: Map[String, T]): () => T = {
    addFlag(autoName, default, spec, FlagType(None, Some(clssByName)))
  }

  private def addFlagName(name: String, flagInfo: FlagInfo) {
    if (names.contains(name)) {
      throw new IllegalArgumentException(s"Duplicate flag name: '$name'")
    }
    names = names + (name -> flagInfo)
  }

  private def addFlag[T](autoName: String, default: Option[T], spec: Option[OptionSpec], flagType: FlagType): () => T = {
    val mainName = spec.map(spec => if (spec.name != "") Some(spec.name) else None).flatten.getOrElse(autoName)
    val altNames = spec.map(spec => if (spec.shortName != '\0') Set(spec.shortName.toString) else Set[String]()).getOrElse(Set())
    val names = altNames + mainName
    val flagInfo = FlagInfo(mainName, flagType, spec)
    for (name <- names) addFlagName(name, flagInfo)
    if (default.isEmpty) {
      required = required :+ names
    }
    () => values.get(mainName).map(_.asInstanceOf[T]).orElse(default).get
  }

  private def help: String = {
    val uniqueNames = names.groupBy(_._2.mainName).map(_._2.head)
    val helps = for (info <- uniqueNames.values) yield {
      // TODO: Make better!
      val optType = info.flagType.cls
        .map(_.runtimeClass.getSimpleName)
        .orElse(info.flagType.enum.map(_.headOption.map(_._2.getClass.getSuperclass.getSimpleName)).flatten)
        .getOrElse("<Unknown>")
      val isRequired = required.exists(_.contains(info.mainName))
      val reqHelp = if (isRequired) " [Required]" else ""
      "--" + info.mainName + ": " + optType + reqHelp + (info.optionSpec match {
        case Some(optionSpec) =>
          if (optionSpec.help == "") "" else "\n    " + optionSpec.help
        case None => ""
      })
    }
    "Usage:\n\n" + helps.mkString("\n\n") + "\n"
  }

  private def exit(isHelp: String, errorMsg: String): Nothing = {
    val helpRx = """^(?:--|-|/)?(?:help|\?)$""".r
    isHelp.toLowerCase match {
      case helpRx() => println(help)
      case _ => println(errorMsg)
    }
    System.exit(1)
    ???  // TODO: How to return Nothing?
  }

  private val stringType = implicitly[ClassTag[String]]

  def parse(args: Seq[String]) {
    def isBoolean(name: String): Boolean = names.get(name).map(_.flagType.cls.map(_ == ClassTag.Boolean)).flatten.getOrElse(true)
    def parseNumber[T](n: String, fn: (String, Int) => T): T = {
      val isHex = n.toLowerCase.startsWith("0x")
      fn(if (isHex) n.drop(2) else n, if (isHex) 16 else 10)
    }
    def add(flags: Map[String, Any], name: String, value: String): Map[String, Any] = {
      val typedValue = names.get(name) match {
        case None => exit(name, s"Unrecognised flag: '$name'")
        case Some(FlagInfo(_, FlagType(Some(cls), None), _)) => cls match {
          case `stringType` => value
          case ClassTag.Boolean => value.toLowerCase match {
            case "true" | "yes" | "1" => true
            case "false" | "no" | "0" => false
            case _ => exit("", s"Invalid boolean value: '$value' for option '$name'")
          }
          case ClassTag.Int => parseNumber(value, Integer.parseInt _)
          case ClassTag.Long => parseNumber(value, java.lang.Long.parseLong _)
          case _ => exit("", s"Cannot handle flag type '$cls' for flag '$name'")
        }
        case Some(FlagInfo(_, FlagType(None, Some(enums)), _)) => enums(value)
        case _ => exit("", s"Illegal state for flag '$name'")
      }
      flags + (names(name).mainName -> typedValue)
    }
    def addShorts(flags: Map[String, Any], names: String, value: Option[String], tail: List[String]): (List[String], Map[String, Any]) = {
      if (isBoolean(names.last.toString) && value.map(_.startsWith("-")).getOrElse(true)) {
        (value.toList ++ tail, names.foldLeft(flags) { (flags, name) => add(flags, name.toString, "true") })
      } else {
        val boolFlags = names.dropRight(1).foldLeft(flags) { (flags, name) => add(flags, name.toString, "true") }
        val allFlags = add(boolFlags, names.last.toString, value.get)
        (tail, allFlags)
      }
    }
    val nameLongRx = """^--(\w[\w-]*)$""".r
    val nameLongValueRx = """^--(\w[\w-]*)=(.*)$""".r
    val namesShortRx = """^-(\w\w*)$""".r
    val namesShortValueRx = """^-(\w\w*)=(.*)$""".r
    def read(args: List[String], flags: Map[String, Any]): Map[String, Any] = args match {

      // Handle booleans first
      case nameLongRx(name) :: value :: tail if isBoolean(name) && value.startsWith("-") =>
        read(value :: tail, add(flags, name, "true"))
      case nameLongRx(name) :: Nil if isBoolean(name) =>
        read(Nil, add(flags, name, "true"))

      // Handle non-booleans
      case nameLongRx(name) :: value :: tail =>
        read(tail, add(flags, name, value))
      case nameLongValueRx(name, value) :: tail =>
        read(tail, add(flags, name, value))

      // Short names
      case namesShortRx(names) :: value :: tail if names.dropRight(1).forall(x => isBoolean(x.toString)) =>
        (read _).tupled(addShorts(flags, names, Some(value), tail))
      case namesShortRx(names) :: Nil if names.forall(x => isBoolean(x.toString)) =>
        (read _).tupled(addShorts(flags, names, None, List()))
      case namesShortValueRx(names, value) :: tail if names.dropRight(1).forall(x => isBoolean(x.toString)) =>
        (read _).tupled(addShorts(flags, names, Some(value), tail))

      case Nil => flags
      case s :: tail => exit(s, s"Unexpected cmd-line arg: '$s'")
    }
    values = read(args.toList, Map())
    required.filterNot(name => name.exists(x => values.contains(x))) match {
      case Nil => // All required flags given
      case s => exit("", s"Required options not present: [${s.map(n => names(n.head).mainName).mkString(", ")}]")
    }
    // Merge values from registrations
    mergeValues()
  }
}
