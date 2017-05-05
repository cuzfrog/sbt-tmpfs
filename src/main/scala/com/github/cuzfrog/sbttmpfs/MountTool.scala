package com.github.cuzfrog.sbttmpfs

import java.io.File
import sbt.Keys._

private object MountTool {
  def mount(file: File, tmpfsDirectory: Option[File], ramSize:Int): Unit = {
    val logger = streams.value.log
    tmpfsDirectory match {
      case None =>
        sbt.Process(s"sudo mount -t tmpfs -o size=${ramSize}m tmpfs ${file.getAbsolutePath}") ! logger
      case Some(tmpfsDir) =>
    }
  }
}
