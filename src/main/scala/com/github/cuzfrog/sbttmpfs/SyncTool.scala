package com.github.cuzfrog.sbttmpfs

import java.io.File

import ExMethod._
import sbt._

import scala.sys.process.Process

/**
  * Created by cuz on 17-5-7.
  */
private object SyncTool {
  def syncByLink(mappingDirs: Map[File, Seq[File]], baseTmpfsDirectory: File)(implicit logger: Logger): Unit = {
    mappingDirs.toSeq.flatMap { case (src, dests) => dests.map(src -> _) }
      .filter(check).foreach { case (src, dest) =>

      //if dest not tmpfs yet, link it.
      if (!dest.isActiveLink && !dest.isOfTmpfs) {
        //if link failed, abort sync.
        if (LinkTool.linkOne(dest, baseTmpfsDirectory).isDefined) {
          logger.debug(s"[SbtTmpfsPlugin] link failed, abort sync. Dest path: $dest")
          return
        }
      }

      sync(src, dest)
    }
  }

  def syncByMount(mappingDirs: Map[File, Seq[File]], mountCmd: String)(implicit logger: Logger): Unit = {
    mappingDirs.toSeq.flatMap { case (src, dests) => dests.map(src -> _) }
      .filter(check).foreach { case (src, dest) =>

      if (!dest.exists) IO.createDirectory(dest)

      //if dest not tmpfs yet, mount it.
      if (!dest.isActiveLink && !dest.isOfTmpfs) {
        //if mounting failed, abort sync.
        if (MountTool.mountOne(dest, mountCmd).isDefined) {
          logger.debug(s"[SbtTmpfsPlugin] mount failed, abort sync. Dest path: $dest")
          return
        }
      }

      sync(src, dest)
    }
  }


  private def check(in: (File, File))(implicit logger: Logger): Boolean = {
    val (src, dest) = in
    if (!src.exists || !src.isDirectory) {
      logger.warn(s"[SbtTmpfsPlugin] source dir does not exist or is not a dir, abort sync." +
        s" Path: ${src.getAbsolutePath}")
      return false
    }

    if (dest.exists && !dest.isLink && !dest.isDirectory) {
      logger.warn(s"[SbtTmpfsPlugin] dest dir exists, but not a link or dir," +
        s" so should not overwrite it, abort sync. Path: ${src.getAbsolutePath}")
      return false
    }

    true
  }

  //see fileSyncTest/FileSyncTest.md.
  private def sync(src: File, dest: File)(implicit logger: Logger): Unit = {
    logger.debug(s"[SbtTmpfsPlugin] sync from $src to $dest.")
    Process(s"""cp -au ${src.getAbsolutePath}/. ${dest.getAbsolutePath}""").!
  }
}
