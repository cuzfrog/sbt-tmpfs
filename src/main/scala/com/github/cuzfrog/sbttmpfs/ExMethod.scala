package com.github.cuzfrog.sbttmpfs

import java.io.File
import java.nio.file.Files

import sbt.{IO, Logger}

import scala.sys.process.{Process, ProcessLogger}

/**
 * Created by cuz on 17-5-7.
 */
private object ExMethod {
  implicit final class ExFile(f: File)(implicit logger: Logger) {
    def isLink: Boolean = Files.isSymbolicLink(f.toPath)

    def isOfTmpfs: Boolean = {
      if (!f.exists) {
        logger.debug(s"check if ${f.getAbsolutePath} is of tmpfs, while the file does not exist.")
        return false
      }
      val existingTmpfsDirs = Process("df").!!.split(raw"""${IO.Newline}""")
        .filter(_.startsWith("""tmpfs """)).flatMap(_.split("""\s""")).filter(_.startsWith("""/"""))

      existingTmpfsDirs.exists(tpath => f.getAbsolutePath.startsWith(tpath))
    }

    def isActiveLink: Boolean = f.isLink && Files.isDirectory(f.toPath)

    def getLinkTarget: Option[File] = {
      if (!f.isActiveLink) {
        logger.debug(s"${f.getAbsolutePath} is not an active link, which has no link target.")
        return None
      }
      Some(f.getCanonicalFile)
    }
  }

  implicit final class ExString(s: String) {
    def isDefined: Boolean = s != null && s.nonEmpty
  }

  implicit final class LoggerEx(in: Logger) {
    def wrapMyLogger: MyLogger = in match {
      case myLogger: MyLogger => myLogger
      case logger => new MyLogger(in)
    }

    def toProcessLogger: ProcessLogger = new ProcessLogger {
      override def err(s: => String): Unit = in.error(s)
      override def out(s: => String): Unit = in.info(s)
      override def buffer[T](f: => T): T = f
    }
  }
}
