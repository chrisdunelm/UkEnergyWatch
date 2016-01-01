package org.ukenergywatch.utils

trait LogComponent {
  this: ClockComponent =>

  def log: Log

  trait Log {

    sealed trait Level
    object Level {
      case object Debug extends Level
      case object Info extends Level
      case object Warn extends Level
      case object Error extends Level
      case object Fatal extends Level
    }

    protected def finalLog(msg: String): Unit

    protected def log(level: Level, msg: String): Unit = {
      val now = clock.nowUtc()
      val logMsg = s"$now [$level] $msg"
      finalLog(logMsg)
    }

    def debug(msg: String): Unit = log(Level.Debug, msg)
    def info(msg: String): Unit = log(Level.Info, msg)
    def warn(msg: String): Unit = log(Level.Warn, msg)
    def error(msg: String): Unit = log(Level.Error, msg)
    def fatal(msg: String): Unit = log(Level.Fatal, msg)

  }

}

trait LogMemoryComponent extends LogComponent {
  this: ClockComponent =>
  object log extends Log {
    var msgs: Vector[String] = Vector.empty
    def finalLog(msg: String): Unit = {
      msgs = msgs :+ msg
    }
  }
}

trait LogFileComponent extends LogComponent {
  this: ClockComponent with FlagsComponent =>
  object log extends Log {
    import java.nio.file.{ FileSystems, Path, Files, StandardOpenOption, StandardCopyOption }
    import org.ukenergywatch.utils.StringExtensions._

    object Flags extends FlagsBase {
      val logDirectoryPath = flag[String](name = "logDirectoryPath")
      val logFilenamePrefix = flag[String](name = "logFilenamePrefix", defaultValue = "log")
      val logFileSize = flag[Int](name = "logFileSize", defaultValue = 1024 * 1024)
      val logFileCount = flag[Int](name = "logFileCount", defaultValue = 25)
    }
    // Filename written to will be <path>/<prefix>.log
    // When full, moved to <path>/prefix.0000.log
    // Will move files up as required on rollover, deleting oldest if required

    lazy val logDirectoryPath = FileSystems.getDefault.getPath(Flags.logDirectoryPath())

    def finalLog(msg: String): Unit = synchronized {
      val writePath: Path = logDirectoryPath.resolve(f"${Flags.logFilenamePrefix()}.log")
      def writeOfsPath(ofs: Int): Path =
        logDirectoryPath.resolve(f"${Flags.logFilenamePrefix()}.$ofs%04d.log")
      val size0 = if (Files.exists(writePath)) Files.size(writePath) else -1
      if (size0 > Flags.logFileSize()) {
        // Move all files up a number, replacing oldest if exists
        for (i <- (Flags.logFileCount() - 2) to 0 by -1) {
          if (Files.exists(writeOfsPath(i))) {
            Files.move(writeOfsPath(i), writeOfsPath(i + 1), StandardCopyOption.REPLACE_EXISTING)
          }
        }
        Files.move(writePath, writeOfsPath(0), StandardCopyOption.REPLACE_EXISTING)
      }
      Files.write(writePath, s"$msg\n".toByteArrayUtf8, StandardOpenOption.APPEND, StandardOpenOption.CREATE)
    }
  }
  log.Flags // Early initialise flags object
}
