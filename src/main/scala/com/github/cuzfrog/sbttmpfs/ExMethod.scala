package com.github.cuzfrog.sbttmpfs

import java.io.File

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
      Process(s"find ${f.getAbsolutePath} -type l").!!.isDefined
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

    def isActiveLink: Boolean = {
      if (!f.isLink) return false
      Process(s"find ${f.getAbsolutePath} -xtype l").!!.isDefined.unary_!
    }


    def getLinkTarget: Option[File] = {
      if (!f.isActiveLink) {
        logger.debug(s"[SbtTmpfsPlugin] ${f.getAbsolutePath} is not an active link, which has no link target.")
        return None
      }

      val linkTargetPath = Process(s"readlink -f ${f.getAbsolutePath}").!!
      Some(new File(linkTargetPath))
    }
  }

  implicit class ExString(s: String) {
    def isDefined: Boolean = s != null && s.nonEmpty
  }
}
