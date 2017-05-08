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

    val tmpfsOn = taskKey[Unit]("Link or mount tmpfs.")
    val tmpfsLink =
      taskKey[Unit]("Link tmpfs to cross-target or user defined directories.")
    val tmpfsMount = taskKey[Unit]("Mount tmpfs point to target.")
    val tmpfsDirectoryMode =
      settingKey[TmpfsDirectoryMode]("Control mount target or link dir within target. Default: Symlink")
    val tmpfsMappingDirectories =
      settingKey[Map[sbt.File, sbt.File]](
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
      tmpfsMappingDirectories := Map.empty,
      cleanKeepFiles ++= tmpfsMappingDirectories.value.values.toSeq
    )
  }

  import autoImport._

  private val taskDefinition = Seq(
    tmpfsLink := {
      implicit val logger = streams.value.log
      val mode = tmpfsDirectoryMode.value
      if (mode == TmpfsDirectoryMode.Symlink) {
        LinkTool.link(tmpfsLinkDirectories.value, tmpfsLinkBaseDirectory.value)
      } else logger.debug(s"[SbtTmpfsPlugin] call tmpfsLink, but mode is: $mode, abort.")
    },
    tmpfsMount := {
      implicit val logger = streams.value.log
      val mode = tmpfsDirectoryMode.value
      if (mode == TmpfsDirectoryMode.Mount) {
        MountTool.mount(tmpfsMountDirectories.value, tmpfsMountCommand.value)
      } else logger.debug(s"[SbtTmpfsPlugin] call tmpfsMount, but mode is: $mode abort.")
    },
    tmpfsSyncMapping := Def.taskDyn {
      implicit val logger = streams.value.log
      val mode = tmpfsDirectoryMode.value
      logger.debug(s"[SbtTmpfsPlugin] sync mapping with mode: $mode")
      mode match {
        case TmpfsDirectoryMode.Symlink => Def.task {
          SyncTool.syncByLink(tmpfsMappingDirectories.value, tmpfsLinkBaseDirectory.value)
        }
        case TmpfsDirectoryMode.Mount => Def.task {
          SyncTool.syncByMount(tmpfsMappingDirectories.value, tmpfsMountCommand.value)
        }
      }
    }.value,
    tmpfsOn := Def.taskDyn {
      tmpfsDirectoryMode.value match {
        case TmpfsDirectoryMode.Symlink => tmpfsLink
        case TmpfsDirectoryMode.Mount => tmpfsMount
      }
    }.value
  )

  private val tmpfsLinkChain = taskKey[Unit]("")

  private val taskDependentRelationships = Seq(
    tmpfsLink := (tmpfsLink runBefore compile.in(Compile)).value,
    tmpfsLink := (tmpfsLink triggeredBy clean).value,
    tmpfsSyncMapping := (tmpfsSyncMapping triggeredBy(tmpfsOn, tmpfsLink, tmpfsMount)).value
  )

  override def trigger: PluginTrigger = allRequirements
  override val projectSettings: Seq[Def.Setting[_]] =
    defaultSbtTmpfsSettings ++ taskDefinition ++ taskDependentRelationships
}
