package com.github.cuzfrog.sbttmpfs

import sbt.{Def, _}
import Keys._

object SbtTmpfsPlugin extends AutoPlugin {

  object autoImport {
    sealed trait TmpfsDirectoryMode
    object TmpfsDirectoryMode {
      case object Symlink extends TmpfsDirectoryMode
      case object Mount extends TmpfsDirectoryMode
    }

    val tmpfsOn =
      taskKey[Unit]("Link tmpfs to cross-target or user defined directories, or mount tmpfs point to target.")
    val tmpfsDirectoryMode =
      settingKey[TmpfsDirectoryMode]("Control mount target or link dir within target. Default: Symlink")
    val tmpfsMappingDirectories =
      settingKey[Map[File, File]](
        """|Keys are source directories that will be synchronized to tmpfs.
           |Values are destination dirs where keys are synchronized to.
           |  If destination dir is within tmpfs or is an active symlink, then only do the sync.
           |  Else, link/mount the destination to/with tmpfs first, then do the sync.
           |Default is empty.
        """.stripMargin
      )
    val tmpfsSyncMapping =
      taskKey[Unit]("Synchronize dirs defined in tmpfsMappingDirectories.")

    // --------- link -------- keys --------
    val tmpfsLinkDirectories =
      settingKey[Seq[File]]("Directories that will be linked to tmpfs. Show this key to see default.")
    val tmpfsLinkBaseDirectory =
      settingKey[File]("Base directory to contain linked target dirs. Default is sbt.IO.temporaryDirectory/sbttmpfs.")

    // --------- mount -------- keys --------
    val tmpfsMountDirectories =
      settingKey[Seq[File]]("Directories that will be mount with tmpfs. Default is target dir.")
    val tmpfsMountCommand =
      settingKey[String]("Default: 'sudo mount -t tmpfs -o size={tmpfsMountSize}m tmpfs' + dirPath.")
    val tmpfsMountSizeLimit = settingKey[Int]("How much RAM limit to tmpfs. In MB. Default: 256m.")

    private val extraTargetDirList = Seq("resolution-cache")
    @volatile lazy val defaultSbtTmpfsSettings: Seq[Def.Setting[_]] = Seq(
      tmpfsDirectoryMode := TmpfsDirectoryMode.Symlink,
      tmpfsLinkDirectories := crossTarget.value +: extraTargetDirList.map(target.value / _),
      tmpfsLinkBaseDirectory := sbt.IO.temporaryDirectory / "sbttmpfs",
      tmpfsMountDirectories := Seq(target.value),
      tmpfsMountSizeLimit := 256,
      tmpfsMountCommand := s"sudo mount -t tmpfs -o size=${tmpfsMountSizeLimit.value}m tmpfs",
      tmpfsMappingDirectories := Map.empty
    )
  }

  import autoImport._

  private val taskDefinition = Seq(
    tmpfsOn := {
      implicit val logger = streams.value.log
      val mode = tmpfsDirectoryMode.value
      logger.debug(s"[SbtTmpfsPlugin] execute task tmpfsOn, mode is $mode.")
      mode match {
        case TmpfsDirectoryMode.Symlink =>
          LinkTool.link(tmpfsLinkDirectories.value, tmpfsLinkBaseDirectory.value)
        case TmpfsDirectoryMode.Mount =>
          MountTool.mount(tmpfsMountDirectories.value, tmpfsMountCommand.value)
      }
    },
    tmpfsSyncMapping := {
      implicit val logger = streams.value.log
      tmpfsDirectoryMode.value match {
        case TmpfsDirectoryMode.Symlink =>
          SyncTool.syncByLink(tmpfsMappingDirectories.value, tmpfsLinkBaseDirectory.value)
        case TmpfsDirectoryMode.Mount =>
          SyncTool.syncByMount(tmpfsMappingDirectories.value, tmpfsMountCommand.value)
      }
    }
  )

  private val taskDependentRelationships = Seq(
    tmpfsOn := (tmpfsOn runBefore compile.in(Compile)).value,
    tmpfsOn := (tmpfsOn triggeredBy clean).value,
    tmpfsSyncMapping := (tmpfsSyncMapping triggeredBy tmpfsOn).value
  )

  override def trigger: PluginTrigger = allRequirements
  override val projectSettings: Seq[Def.Setting[_]] =
    defaultSbtTmpfsSettings ++ taskDefinition ++ taskDependentRelationships
}
