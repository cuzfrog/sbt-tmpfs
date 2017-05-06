package com.github.cuzfrog.sbttmpfs

import java.io.File
import java.nio.file.Paths

import sbt._

import scala.collection.concurrent.TrieMap

private object LinkTool {

  //symlinks and their target tmpfs dir, used for clean old links after task clean.
  private val linkedDirsRecord = TrieMap.empty[File, File]

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

      if (output.isDefined) {
        logger.error(s"[SbtTmpfsPlugin] tmpfs link failed with info: $output")
      } else {
        //if linking is successful, record symlink and its linked-target tmpfs dir.
        //And delete the previous linked-target tmpfs dir.
        linkedDirsRecord.put(targetDir, tmpfsDir).foreach(IO.delete)
      }
    }
  }
  /**
    * Try to mount target directory with tmpfs.
    * <br><br>
    * If target is not directory or does not exist, abort.
    * If target is already of tmpfs, abort.
    *
    * @param targetDir the target directory is about to mount to.
    * @param mountCmd  the shell mount command string.
    * @param logger    sbt logger.
    */
  def mount(targetDir: File, mountCmd: String)(implicit logger: Logger): Unit = {
    if (!targetDir.isDirectory) {
      logger.warn(s"[SbtTmpfsPlugin] targetDir is not a directory," +
        s" abort mounting tmpfs. Path:${targetDir.getCanonicalPath}")
      return
    }

    if (targetDir.isOfTmpfs) {
      logger.debug("[SbtTmpfsPlugin] targetDir is already of tmpfs, abort mounting.")
      return
    }

    val output = Process(s"mountCmd ${targetDir.getCanonicalPath}").!!
    if (output.isDefined) logger.error(s"[SbtTmpfsPlugin] tmpfs mount failed with info: $output")
  }

  // -------------------------- Helpers --------------------------
  private implicit class ExFile(f: File)(implicit logger: Logger) {
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


    def getLinkTarget: Option[File] = {
      if (!f.isActiveLink) {
        logger.debug(s"[SbtTmpfsPlugin] ${f.getCanonicalPath} is not an active link, which has no link target.")
        return None
      }

      val linkTargetPath = Process(s"readlink -f ${f.getCanonicalPath}").!!
      Some(new File(linkTargetPath))
    }
  }

  private implicit class ExString(s: String) {
    def isDefined: Boolean = s != null && s.nonEmpty
  }
}
