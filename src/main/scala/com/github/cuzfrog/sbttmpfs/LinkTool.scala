package com.github.cuzfrog.sbttmpfs

import java.io.File
import java.nio.file.Paths

import sbt._

import scala.collection.concurrent.TrieMap
import ExMethod._

private object LinkTool {

  //symlinks and their target tmpfs dir, used for clean old links after task clean.
  private val linkedDirsRecord = TrieMap.empty[String, File]

  /**
    * Try to symbolic-link target directories into base tmpfs directory.
    * <br><br>
    * If target directory is an active symlink or already of tmpfs, abort.
    * If base tmpfs directory is actually not of tmpfs, abort and warn.
    * <br><br>
    * If target directory already exists, move its contents(if it has)
    * to newly created dir inside base tmpfs dir. And then create the link.
    * If target is a dead link, delete it first.
    *
    * @param targetDirs         seq of target dirs to be linked.
    * @param baseTmpfsDirectory a base dir that should be of tmpfs.
    * @param logger             sbt logger.
    */
  def link(targetDirs: Seq[File], baseTmpfsDirectory: File)(implicit logger: Logger): Unit = this.synchronized {
    targetDirs.foreach { targetDir =>
      linkOne(targetDir, baseTmpfsDirectory)
    }
  }

  /** Return error message if failed. */
  def linkOne(targetDir: File, baseTmpfsDirectory: File)(implicit logger: Logger): String = {
    if (targetDir.isActiveLink || targetDir.isOfTmpfs) {
      val msg = s"[SbtTmpfsPlugin] $targetDir is already an active symlink or of tmpfs, abort linking."
      logger.debug(msg)
      return msg
    }

    if (!baseTmpfsDirectory.exists) IO.createDirectory(baseTmpfsDirectory)

    if (!baseTmpfsDirectory.isOfTmpfs) {
      val msg = s"[SbtTmpfsPlugin]Base directory:${baseTmpfsDirectory.getAbsolutePath}" +
        s" is not of tmpfs. Abort linking. Please mount it with tmpfs first."
      logger.warn(msg)
      return msg
    }

    @volatile lazy val tmpfsDir = {
      val randomBaseDir = IO.createUniqueDirectory(baseTmpfsDirectory)
      val f = Paths.get(randomBaseDir.getAbsolutePath, targetDir.getName).toFile
      IO.createDirectory(f)
      logger.debug("[SbtTmpfsPlugin] new tmpfs dir created:" + f.getAbsolutePath)
      f
    }

    if (targetDir.exists) {
      if (!targetDir.isLink) IO.copyDirectory(targetDir, tmpfsDir)
      IO.delete(targetDir)
    }

    if (!targetDir.getParentFile.exists) IO.createDirectory(targetDir.getParentFile)

    val cmd = s"ln -snf ${tmpfsDir.getAbsolutePath} ${targetDir.getParent}/"
    logger.debug("[SbtTmpfsPlugin] Try to link, execute shell command:" + cmd)
    val output = Process(cmd) !!

    if (output.isDefined) {
      logger.error(s"[SbtTmpfsPlugin] tmpfs link failed with info: $output")
      output
    } else {
      //if linking is successful, record symlink and its linked-target tmpfs dir.
      //And delete the previous linked-target tmpfs dir.
      linkedDirsRecord.put(targetDir.getAbsolutePath, tmpfsDir).foreach { old =>
        IO.delete(old.getParentFile)
      }
      ""
    }
  }

  def cleanDeadLinks(links: Seq[File])(implicit logger: Logger): Unit = {
    if(links == null) return
    links.foreach { syml =>
      if (syml.isLink && !syml.isActiveLink) {
        IO.delete(syml)
        logger.debug(s"[SbtTmpfsPlugin] $syml is a dead link. deleted.")
      }
    }
  }
}
