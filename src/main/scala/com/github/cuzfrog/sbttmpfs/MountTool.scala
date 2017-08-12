package com.github.cuzfrog.sbttmpfs

import java.io.File

import ExMethod._
import sbt._

import scala.sys.process.Process

private object MountTool {

  /**
    * Try to mount target directories with tmpfs.
    * <br><br>
    * If target is not directory or does not exist, abort.
    * If target is already of tmpfs, abort.
    *
    * @param targetDirs the target directories to mount with tmpfs.
    * @param mountCmd   the shell mount command string.
    * @param logger     sbt logger.
    */
  def mount(targetDirs: Seq[File], mountCmd: String)(implicit logger: Logger): Unit = {
    targetDirs.foreach { targetDir =>
      mountOne(targetDir, mountCmd)
    }
  }

  /** Return error message if failed. */
  def mountOne(targetDir: File, mountCmd: String)(implicit logger: Logger): String = {
    if (!targetDir.isDirectory) {
      val msg = s"[SbtTmpfsPlugin] targetDir is not a directory," +
        s" abort mounting tmpfs. Path:${targetDir.getAbsolutePath}"
      logger.warn(msg)
      return msg
    }

    if (targetDir.isOfTmpfs) {
      val msg = s"[SbtTmpfsPlugin] $targetDir is already of tmpfs, abort mounting."
      logger.debug(msg)
      return msg
    }
    val cmd = s"$mountCmd ${targetDir.getAbsolutePath}"
    logger.debug("[SbtTmpfsPlugin] Try to mount, execute shell command:" + cmd)
    val output = Process(cmd).!!
    if (output.isDefined) {
      logger.error(s"[SbtTmpfsPlugin] tmpfs mount failed with info: $output")
      output
    }
    ""
  }
}
