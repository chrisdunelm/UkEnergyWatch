package org.ukenergywatch.utils

import java.nio.file.{ Path, Files, SimpleFileVisitor, FileVisitResult }
import java.nio.file.attribute.BasicFileAttributes

object FileExtensions {

  implicit class RichPath(val path: Path) extends AnyVal {

    // Delete file/dir and sub-files/dirs
    def subDelete(): Unit = {
      Files.walkFileTree(path, new SimpleFileVisitor[Path] {
        override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
          Files.delete(file)
          FileVisitResult.CONTINUE
        }
        override def postVisitDirectory(dir: Path, e: java.io.IOException): FileVisitResult = {
          Files.delete(dir)
          FileVisitResult.CONTINUE
        }
      })
    }

  }

}
