package com.github.cuzfrog.sbttmpfs

import java.io.File

import com.github.cuzfrog.sbttmpfs.ExMethod._
import sbt._

import scala.sys.process.Process
import scala.util.{Failure, Success, Try}

private object MountTool {

  /**
   * Try to mount target directories with tmpfs.
   * <br><br>
   * If target is not directory or does not exist, abort.
   * If target is already of tmpfs, abort.
   * When no tty present and no askpass program specified, mount will fail.
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
      val msg = "targetDir is not a directory," +
        s" abort mounting tmpfs. Path:${targetDir.getAbsolutePath}"
      logger.warn(msg)
      return msg
    }

    if (targetDir.isOfTmpfs) {
      val msg = s"$targetDir is already of tmpfs, abort mounting."
      logger.debug(msg)
      return msg
    }

    val cmd = s"$mountCmd ${targetDir.getAbsolutePath}"
    logger.debug("Try to mount, execute shell command:" + cmd)

    Try(Process(cmd) !! logger.toProcessLogger) match {
      case Success(stdout) =>
        if (stdout.isDefined) {
          logger.error(s"tmpfs mount failed with info: $stdout")
        }
        stdout
      case Failure(t) =>
        logger.error("tmpfs mount failed with non-zero exit code")
        t.getMessage
    }
  }

}
