package com.github.cuzfrog.sbttmpfs

import sbt._
import Keys._

object SbtTmpfsPlugin extends AutoPlugin {
  private val extraTargetDirList = Seq(
    "resolution-cache", "streams"
  )

  object autoImport {
    val tmpfsLinkTarget = taskKey[Unit]("Link tmpfs to cross-target or user defined directories.")
    val tmpfsTargetDirectories =
      settingKey[Seq[File]]("Directories that will be linked to tmpfs. Default dirs include:"
        + IO.Newline + extraTargetDirList.mkString(IO.Newline))
    val tmpfsBaseDirectory =
      settingKey[File]("Base directory to contain target dirs. Default is sbt.IO.temporaryDirectory/sbttmpfs.")

    @volatile lazy val baseSbtTmpfsSettings: Seq[Def.Setting[_]] = Seq(
      tmpfsLinkTarget := {
        doLink(
          (tmpfsTargetDirectories in tmpfsLinkTarget).value,
          (tmpfsBaseDirectory in tmpfsLinkTarget).value,
          streams.value.log)
      },
      tmpfsTargetDirectories in tmpfsLinkTarget :=
        crossTarget.value +: extraTargetDirList.map(target.value / _),
      tmpfsBaseDirectory in tmpfsLinkTarget := sbt.IO.temporaryDirectory / "sbttmpfs",
      initialize := {
        val logger = sLog.value
        logger.debug("[SbtTmpfsPlugin] try to link during initialization.")
        doLink(
          (tmpfsTargetDirectories in tmpfsLinkTarget).value,
          (tmpfsBaseDirectory in tmpfsLinkTarget).value,
          logger)
        initialize.value
      }
    )
  }

  private def doLink(tmpfsTgtDirs: Seq[File], base: File, logger: Logger): Unit = {
    val tool = new LinkTool(logger)
    tmpfsTgtDirs.foreach(tool.link(_, base))
  }


  import autoImport._

  override def trigger: PluginTrigger = allRequirements
  override val projectSettings =
    inConfig(Compile)(baseSbtTmpfsSettings)
}
