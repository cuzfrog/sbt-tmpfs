package com.github.cuzfrog.sbttmpfs

import sbt._
import Keys._

object SbtTmpfsPlugin extends AutoPlugin {
  object autoImport {
    val tmpfsLinkTarget = taskKey[Unit]("Link tmpfs to cross-target or user defined directories.")
    val tmpfsTargetDirectories = settingKey[Seq[File]]("Directories that will be linked to tmpfs.")
    val tmpfsBaseDirectory = settingKey[File]("Base directory to contain target dirs. Default is sbt.IO.temporaryDirectory.")
    val tmpfsAutoLink = settingKey[Seq[File]]("Directories that will be linked to tmpfs.")

    lazy val baseSbtTmpfsSettings: Seq[Def.Setting[_]] = Seq(
      tmpfsLinkTarget := {
        (tmpfsTargetDirectories in tmpfsLinkTarget).value.foreach(MountTool.mount(_,
          (tmpfsBaseDirectory in tmpfsLinkTarget).value,
          streams.value.log)
        )
      },
      tmpfsTargetDirectories in tmpfsLinkTarget := Seq(target.value),
      tmpfsBaseDirectory in tmpfsLinkTarget := sbt.IO.temporaryDirectory
    )
  }

  import autoImport.baseSbtTmpfsSettings

  override def trigger: PluginTrigger = allRequirements
  override val projectSettings =
    inConfig(Compile)(baseSbtTmpfsSettings) ++ inConfig(Test)(baseSbtTmpfsSettings)
}
