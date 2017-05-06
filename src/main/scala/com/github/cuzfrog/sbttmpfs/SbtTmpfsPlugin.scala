package com.github.cuzfrog.sbttmpfs

import sbt._
import Keys._

object SbtTmpfsPlugin extends AutoPlugin {
  private val extraTargetDirList = Seq("resolution-cache", "streams")

  object autoImport {
    sealed trait TmpfsDirectoryMode
    object TmpfsDirectoryMode {
      case object Symlink extends TmpfsDirectoryMode
      case object Mount extends TmpfsDirectoryMode
    }

    val tmpfsOn =
      taskKey[Unit]("Link tmpfs to cross-target or user defined directories, or mount tmpfs point to target.")
    val tmpfsLinkDirectories =
      settingKey[Seq[File]]("Directories that will be linked to tmpfs. Default dirs include:"
        + IO.Newline + extraTargetDirList.mkString(IO.Newline))
    val tmpfsBaseDirectory =
      settingKey[File]("Base directory to contain linked target dirs. Default is sbt.IO.temporaryDirectory/sbttmpfs.")
    val tmpfsDirectoryMode =
      settingKey[TmpfsDirectoryMode]("Control mount target or link dir within target. Default: Symlink")
    val tmpfsMountCommand =
      settingKey[String]("Default: 'sudo mount -t tmpfs -o size={tmpfsMountSize}m tmpfs' + dirPath.")
    val tmpfsMountSize = settingKey[Int]("How much RAM limit to tmpfs. In MB. Default: 256m.")

    @volatile lazy val baseSbtTmpfsSettings: Seq[Def.Setting[_]] = Seq(
      tmpfsOn := {
        tmpfsDirectoryMode.value match {
          case TmpfsDirectoryMode.Symlink =>
            LinkTool.link(tmpfsLinkDirectories.value, tmpfsBaseDirectory.value)(streams.value.log)
          case TmpfsDirectoryMode.Mount =>
            LinkTool.mount(target.value, tmpfsMountCommand.value)
        }
      },
      tmpfsLinkDirectories := crossTarget.value +: extraTargetDirList.map(target.value / _),
      tmpfsBaseDirectory := sbt.IO.temporaryDirectory / "sbttmpfs",
      tmpfsMountSize := 256,
      tmpfsMountCommand := s"sudo mount -t tmpfs -o size=${tmpfsMountSize.value}m tmpfs",
      tmpfsDirectoryMode := TmpfsDirectoryMode.Symlink,
      tmpfsOn := (tmpfsOn runBefore (compile in Compile)).value,
      tmpfsOn := (tmpfsOn triggeredBy clean).value
    )
  }

  import autoImport._

  override def trigger: PluginTrigger = allRequirements
  override val projectSettings =
    inConfig(Compile)(baseSbtTmpfsSettings)
}
