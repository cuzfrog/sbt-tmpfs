package com.github.cuzfrog.sbttmpfs

import sbt._
import Keys._

object SbtTmpfsPlugin extends AutoPlugin {
  object autoImport {
    val tmpfsTargetMount = taskKey[Unit]("Mount tmpfs to cross-target or user defined directories.")
    val tmpfsTargetDirectories = settingKey[Seq[File]]("Directories that will be linked to tmpfs.")
    val tmpfsPoolDirectory = settingKey[Option[File]]("Tmpfs directory to contain target dirs." +
      " If None, then try to mount new point. Default is None.")
    val tmpfsRamSizeInMB = settingKey[Int]("How many MB every directory will acquire. Default is 128MB.")

    lazy val baseSbtTmpfsSettings: Seq[Def.Setting[_]] = Seq(
      tmpfsTargetMount := {
        tmpfsTargetDirectories.value.foreach(MountTool.mount(_, tmpfsPoolDirectory.value, tmpfsRamSizeInMB.value))
      },
      tmpfsTargetDirectories in tmpfsTargetMount := Seq(crossTarget.value),
      tmpfsPoolDirectory in tmpfsTargetMount := None,
      tmpfsRamSizeInMB in tmpfsTargetMount := 128
    )
  }

  import autoImport.baseSbtTmpfsSettings

  override val projectSettings =
    inConfig(Compile)(baseSbtTmpfsSettings) ++ inConfig(Test)(baseSbtTmpfsSettings)
}
