package com.github.cuzfrog.sbttmpfs

import java.io.File
import java.nio.file.Paths

import sbt._

private class LinkTool(logger: Logger) {
  def link(targetDir: File, baseTmpfsDirectory: File): Unit = {
    if (targetDir.isActiveLink || targetDir.isOfTmpfs) {
      logger.debug("[SbtTmpfsPlugin] targetDir is already an active symlink or of tmpfs.")
      return
    }

    if (!baseTmpfsDirectory.exists) IO.createDirectory(baseTmpfsDirectory)

    if (!baseTmpfsDirectory.isOfTmpfs) {
      logger.warn(s"[SbtTmpfsPlugin]Base directory:${baseTmpfsDirectory.getAbsolutePath}" +
        s" is not of tmpfs. Abort linking. Please mount it with tmpfs first.")
      return
    }

    @volatile lazy val tmpfsDir = {

      val randomBaseDir = IO.createUniqueDirectory(baseTmpfsDirectory)
      val f = Paths.get(randomBaseDir.getAbsolutePath, targetDir.getName).toFile
      IO.createDirectory(f)
      logger.debug("[SbtTmpfsPlugin] new tmpfs dir created:" + f.getCanonicalPath)
      f
    }

    if (targetDir.exists) {
      if (!targetDir.isLink) IO.copyDirectory(targetDir, tmpfsDir)
      IO.delete(targetDir)
    }

    if (!targetDir.getParentFile.exists) IO.createDirectory(targetDir.getParentFile)

    val cmd = s"ln -snf ${tmpfsDir.getCanonicalPath} ${targetDir.getParent}/"
    logger.debug("[SbtTmpfsPlugin] execute shell command:" + cmd)
    val output = Process(cmd) !!

    if (output != null && output.nonEmpty) logger.error(s"[SbtTmpfsPlugin] tmpfs link failed with info:$output")
  }

  private implicit class ExFile(f: File) {
    def isLink: Boolean = {
      if (!f.exists) {
        logger.debug(s"[SbtTmpfsPlugin] check if ${f.getCanonicalPath} is a link, while the file does not exist.")
        return false
      }
      Process(s"find ${f.getCanonicalPath} -type l").!!.isDefined
    }

    def isOfTmpfs: Boolean = {
      if (!f.exists) {
        logger.debug(s"[SbtTmpfsPlugin] check if ${f.getCanonicalPath} is of tmpfs, while the file does not exist.")
        return false
      }
      val existingTmpfsDirs = Process("df").!!.split(raw"""${IO.Newline}""")
        .filter(_.startsWith("""tmpfs """)).flatMap(_.split("""\s""")).filter(_.startsWith("""/"""))

      existingTmpfsDirs.exists(tpath => f.getAbsolutePath.startsWith(tpath))
    }

    def isActiveLink: Boolean = {
      if (!f.isLink) return false
      Process(s"find ${f.getCanonicalPath} -xtype l").!!.isDefined.unary_!
    }
  }

  private implicit class ExString(s: String) {
    def isDefined: Boolean = s != null && s.nonEmpty
  }
}
