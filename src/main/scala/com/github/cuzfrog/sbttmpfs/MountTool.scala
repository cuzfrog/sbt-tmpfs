package com.github.cuzfrog.sbttmpfs

import java.io.File
import java.nio.file.Paths

import sbt._

private object MountTool {
  def mount(targetDir: File, baseTmpfsDirectory: File, logger: Logger): Unit = {
    val existingTmpfsDirs = Process("df").!!.split(raw"""${IO.Newline}""")
      .filter(_.startsWith("""tmpfs """)).flatMap(_.split("""\s""")).filter(_.startsWith("""/"""))

    if (existingTmpfsDirs.exists(tpath => baseTmpfsDirectory.getAbsolutePath.startsWith(tpath))) {

      val randomBaseDir = IO.createUniqueDirectory(baseTmpfsDirectory)
      val tmpfsDir = {
        val f = Paths.get(randomBaseDir.getAbsolutePath, targetDir.getName).toFile
        IO.createDirectory(f)
        f
      }
      if (!targetDir.exists()) IO.createDirectory(targetDir)
      IO.copyDirectory(targetDir, tmpfsDir)
      IO.delete(targetDir)
      val cmd = s"ln -snf ${tmpfsDir.getCanonicalPath} ${targetDir.getParent}/"
      logger.debug("[SbtTmpfsPlugin] execute shell command:" + cmd)
      val output = Process(cmd) !!
      if (output != null && output.nonEmpty) logger.error(s"[SbtTmpfsPlugin] tmpfs link failed with info:$output")
    }
    else logger.error(s"Base directory:${baseTmpfsDirectory.getAbsolutePath} is not of tmpfs.")
  }
}
