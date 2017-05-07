package com.github.cuzfrog.sbttmpfs

import java.io.File
import java.nio.file.Files

import sbt.{IO, Logger, Process}

/**
  * Created by cuz on 17-5-7.
  */
private object ExMethod {
  implicit class ExFile(f: File)(implicit logger: Logger) {
    def isLink: Boolean = {
      if (!f.exists) {
        logger.debug(s"[SbtTmpfsPlugin] check if ${f.getAbsolutePath} is a link, while the file does not exist.")
        return false
      }
      Files.isSymbolicLink(f.toPath)
    }

    def isOfTmpfs: Boolean = {
      if (!f.exists) {
        logger.debug(s"[SbtTmpfsPlugin] check if ${f.getAbsolutePath} is of tmpfs, while the file does not exist.")
        return false
      }
      val existingTmpfsDirs = Process("df").!!.split(raw"""${IO.Newline}""")
        .filter(_.startsWith("""tmpfs """)).flatMap(_.split("""\s""")).filter(_.startsWith("""/"""))

      existingTmpfsDirs.exists(tpath => f.getAbsolutePath.startsWith(tpath))
    }

    def isActiveLink: Boolean = f.isLink && Files.isDirectory(f.toPath)

    def getLinkTarget: Option[File] = {
      if (!f.isActiveLink) {
        logger.debug(s"[SbtTmpfsPlugin] ${f.getAbsolutePath} is not an active link, which has no link target.")
        return None
      }
      Some(f.getCanonicalFile)
    }
  }

  implicit class ExString(s: String) {
    def isDefined: Boolean = s != null && s.nonEmpty
  }
}
