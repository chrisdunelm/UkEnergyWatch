package org.ukenergywatch.slogger.impl

import org.slf4j.helpers.MarkerIgnoringBase
import org.slf4j.helpers.MessageFormatter.format
import org.slf4j.helpers.MessageFormatter.arrayFormat
import org.slf4j.helpers.FormattingTuple
import org.joda.time._
import org.ukenergywatch.utils.Options
import org.ukenergywatch.utils.OptionSpec


object Slogger {

  object Flags extends Options {
    val logDir = opt("")
    val logLevel = opt[Level](Info)
    val logFilePrefix = opt("")
    val logToStdOut = opt(false)
    val logMaxFileSize = opt(100L * 1024L * 1024L)
    val logKeepDays = opt(30)
    val logExceptionLines = opt(0, OptionSpec(help = "Number of lines of exceptions to log. 0 = all in file, minimal to stdout"))
  }

  sealed abstract class Level(val level: Int)
  case object Trace extends Level(0)
  case object Debug extends Level(1)
  case object Info extends Level(2)
  case object Warn extends Level(3)
  case object Error extends Level(4)
}

class Slogger(name: String) extends MarkerIgnoringBase {
  import Slogger._

  private def logToFile(now: DateTime, s: String, e: Option[String]) {
    import java.io.FileWriter
    val nowStr = now.toString("yyyy'-'MM'-'dd")
    val filename = s"${Flags.logDir()}/${Flags.logFilePrefix()}$nowStr.log"
    try {
      val fw = new FileWriter(filename, true)
      try {
        fw.write(s)
      } finally {
        fw.close()
      }
    } catch {
      case t: Throwable => println(s"Failed to log to file '$filename': $t")
    }
  }

  private def isLevelEnabled(level: Level): Boolean = level.level >= Flags.logLevel().level

  private def log(level: Level)(fn: => FormattingTuple): Unit = {
    if (isLevelEnabled(level)) {
      val logMsg = fn
      val now = DateTime.now(DateTimeZone.UTC)
      val out = s"$level:$now:$name:${logMsg.getMessage}"
      val exStr = Option(logMsg.getThrowable).map { t =>
        import java.io.{StringWriter, PrintWriter}
        val sw = new StringWriter()
        t.printStackTrace(new PrintWriter(sw))
        sw.toString
      }
      if (Flags.logDir() != "") {
        logToFile(now, out, exStr)
      } 
      if (Flags.logToStdOut() || Flags.logDir() == "") {
        println(out)
        if (exStr.nonEmpty) {
          println(exStr.get.split("\n").take(Flags.logExceptionLines() + 1).mkString("\n"))
        }
      }
    }
  }

  def isTraceEnabled(): Boolean = isLevelEnabled(Trace)
  def trace(msg: String): Unit = log(Trace) { new FormattingTuple(msg) }
  def trace(msg: String, t: Throwable): Unit = log(Trace) { format(msg, t) }
  def trace(msg: String, arg: Object): Unit = log(Trace) { format(msg, arg) }
  def trace(msg: String, arg1: Object, arg2: Object): Unit = log(Trace) { format(msg, arg1, arg2) }
  def trace(msg: String, args: Object*): Unit = log(Trace) { arrayFormat(msg, args.toArray) }

  def isDebugEnabled(): Boolean = isLevelEnabled(Debug)
  def debug(msg: String): Unit = log(Debug) { new FormattingTuple(msg) }
  def debug(msg: String, t: Throwable): Unit = log(Debug) { format(msg, t) }
  def debug(msg: String, arg: Object): Unit = log(Debug) { format(msg, arg) }
  def debug(msg: String, arg1: Object, arg2: Object): Unit = log(Debug) { format(msg, arg1, arg2) }
  def debug(msg: String, args: Object*): Unit = log(Debug) { arrayFormat(msg, args.toArray) }

  def isInfoEnabled(): Boolean = isLevelEnabled(Info)
  def info(msg: String): Unit = log(Info) { new FormattingTuple(msg) }
  def info(msg: String, t: Throwable): Unit = log(Info) { format(msg, t) }
  def info(msg: String, arg: Object): Unit = log(Info) { format(msg, arg) }
  def info(msg: String, arg1: Object, arg2: Object): Unit = log(Info) { format(msg, arg1, arg2) }
  def info(msg: String, args: Object*): Unit = log(Info) { arrayFormat(msg, args.toArray) }

  def isWarnEnabled(): Boolean = isLevelEnabled(Warn)
  def warn(msg: String): Unit = log(Warn) { new FormattingTuple(msg) }
  def warn(msg: String, t: Throwable): Unit = log(Warn) { format(msg, t) }
  def warn(msg: String, arg: Object): Unit = log(Warn) { format(msg, arg) }
  def warn(msg: String, arg1: Object, arg2: Object): Unit = log(Warn) { format(msg, arg1, arg2) }
  def warn(msg: String, args: Object*): Unit = log(Warn) { arrayFormat(msg, args.toArray) }

  def isErrorEnabled(): Boolean = isLevelEnabled(Error)
  def error(msg: String): Unit = log(Error) { new FormattingTuple(msg) }
  def error(msg: String, t: Throwable): Unit = log(Error) { format(msg, t) }
  def error(msg: String, arg: Object): Unit = log(Error) { format(msg, arg) }
  def error(msg: String, arg1: Object, arg2: Object): Unit = log(Error) { format(msg, arg1, arg2) }
  def error(msg: String, args: Object*): Unit = log(Error) { arrayFormat(msg, args.toArray) }

}
