package com.github.cuzfrog.sbttmpfs

import java.io.File

import ExMethod._
import sbt._

/**
  * Created by cuz on 17-5-7.
  */
private object MapTool {
  def mapByLink(mappingDirs: Map[File, File], baseTmpfsDirectory: File)(implicit logger: Logger): Unit = {
    mappingDirs.filter(checkBeforeMapping).foreach { case (src, dest) =>
      //If no error message returned, do the copy.
      if (!LinkTool.linkOne(dest, baseTmpfsDirectory).isDefined) IO.copyDirectory(src, dest)
    }
  }

  def mapByMount(mappingDirs: Map[File, File], mountCmd: String)(implicit logger: Logger): Unit = {
    mappingDirs.filter(checkBeforeMapping).foreach { case (src, dest) =>
      if (!MountTool.mountOne(dest, mountCmd).isDefined) IO.copyDirectory(src, dest)
    }
  }

  private def checkBeforeMapping(in: (File, File))(implicit logger: Logger): Boolean = {
    val (src, dest) = in
    if (!src.exists || !src.isDirectory) {
      logger.warn(s"[SbtTmpfsPlugin] source dir does not exist or is not a dir, abort mapping." +
        s" Path: ${src.getAbsolutePath}")
      return false
    }

    if (dest.isActiveLink || dest.isOfTmpfs) {
      logger.debug(s"[SbtTmpfsPlugin] dest $dest is already an active symlink or of tmpfs, abort mapping.")
      return false
    }

    if (dest.exists && !dest.isDirectory) {
      logger.debug(s"[SbtTmpfsPlugin] dest $dest exists but not a dir, abort mapping." +
        s" (also not an active symlink or tmpfs)")
      return false
    }

    true
  }

  def syncMapping(mappingDirs: Map[File, File])(implicit logger: Logger): Unit = {
    mappingDirs.filter(checkBeforeSync).foreach { case (src, dest) =>
      IO.copyDirectory(src, dest)
    }
  }

  private def checkBeforeSync(in: (File, File))(implicit logger: Logger): Boolean = {
    val (src, dest) = in
    if (!src.exists || !src.isDirectory) {
      logger.warn(s"[SbtTmpfsPlugin] source dir does not exist or is not a dir, abort sync mapping." +
        s" Path: ${src.getAbsolutePath}")
      return false
    }

    if (!dest.isActiveLink && !dest.isOfTmpfs) {
      logger.debug(s"[SbtTmpfsPlugin] dest $dest is not an active symlink or of tmpfs, abort sync mapping.")
      return false
    }
    true
  }

}
