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

    val tmpfsOn: TaskKey[Unit] = taskKey[Unit]("Link or mount tmpfs.")
    val tmpfsLink: TaskKey[Unit] =
      taskKey[Unit]("Link tmpfs to cross-target or user defined directories.")
    val tmpfsMount: TaskKey[Unit] = taskKey[Unit]("Mount tmpfs point to target.")
    val tmpfsDirectoryMode: SettingKey[TmpfsDirectoryMode] =
      settingKey[TmpfsDirectoryMode]("Control mount target or link dir within target. Default: Symlink")
    val tmpfsMappingDirectories: SettingKey[Map[sbt.File, Seq[sbt.File]]] =
      settingKey[Map[File, Seq[File]]](
        """|Keys are source directories that will be synchronized to tmpfs.
           |Values are destination dirs where keys are synchronized to.
           |  If destination dir is within tmpfs or is an active symlink, then only do the sync.
           |  Else, link/mount the destination to/with tmpfs first, then do the sync.
           |Default is empty.
        """.stripMargin
      )
    val tmpfsSyncMapping: TaskKey[Unit] =
      taskKey[Unit]("Synchronize dirs defined in tmpfsMappingDirectories.")
    //val tmpfsCleanDeadLinks = taskKey[Unit]("Try to clean dead links.")

    // --------- link -------- keys --------
    val tmpfsLinkDirectories: SettingKey[Seq[sbt.File]] =
      settingKey[Seq[File]]("Directories that will be linked to tmpfs. Show this key to see default.")
    val tmpfsLinkBaseDirectory: SettingKey[sbt.File] =
      settingKey[File]("Base directory to contain linked target dirs. Default is sbt.IO.temporaryDirectory/sbttmpfs.")

    // --------- mount -------- keys --------
    val tmpfsMountDirectories: SettingKey[Seq[sbt.File]] =
      settingKey[Seq[File]]("Directories that will be mount with tmpfs. Default is target dir.")
    val tmpfsMountCommand: SettingKey[String] =
      settingKey[String]("Default: 'sudo mount -t tmpfs -o size={tmpfsMountSize}m tmpfs' + dirPath.")
    val tmpfsMountSizeLimit: SettingKey[Int] =
      settingKey[Int]("How much RAM limit to tmpfs. In MB. Default: 256m.")

    private val extraTargetDirList = Seq("resolution-cache")
    @volatile lazy val defaultSbtTmpfsSettings: Seq[Def.Setting[_]] = Seq(
      tmpfsDirectoryMode := TmpfsDirectoryMode.Symlink,
      tmpfsLinkDirectories := crossTarget.value +: extraTargetDirList.map(target.value / _),
      tmpfsLinkBaseDirectory := sbt.IO.temporaryDirectory / "sbttmpfs",
      tmpfsMountDirectories := Seq(target.value),
      tmpfsMountSizeLimit := 256,
      tmpfsMountCommand := s"sudo mount -t tmpfs -o size=${tmpfsMountSizeLimit.value}m tmpfs",
      tmpfsMappingDirectories := Map.empty,
      cleanKeepFiles ++= {
        val mappingDirs = tmpfsMappingDirectories.value.values.toSeq.flatten
        mappingDirs.filter(_.absolutePath.startsWith(target.value.absolutePath)) //under target
      }
    )
  }

  import autoImport._

  private val taskDefinition = Seq(
    tmpfsLink := Def.taskDyn {
      implicit val logger = streams.value.log
      if (isCi) Def.task(logger.debug("CI environment, abort linking."))
      else {
        val mode = tmpfsDirectoryMode.value
        if (mode == TmpfsDirectoryMode.Symlink) {
          Def.task(LinkTool.link(tmpfsLinkDirectories.value, tmpfsLinkBaseDirectory.value))
        } else Def.task(logger.debug(s"[SbtTmpfsPlugin] call tmpfsLink, but mode is: $mode, abort."))
      }
    }.value,
    tmpfsMount := Def.taskDyn {
      implicit val logger = streams.value.log
      if (isCi) Def.task(logger.debug("CI environment, abort mounting."))
      else {
        val mode = tmpfsDirectoryMode.value
        if (mode == TmpfsDirectoryMode.Mount) {
          Def.task(MountTool.mount(tmpfsMountDirectories.value, tmpfsMountCommand.value))
        } else Def.task(logger.debug(s"[SbtTmpfsPlugin] call tmpfsMount, but mode is: $mode abort."))
      }
    }.value,
    tmpfsSyncMapping := Def.taskDyn {
      implicit val logger = streams.value.log
      if (isCi) Def.task(logger.debug("CI environment, abort sync."))
      else {
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
      }
    }.value,
    tmpfsOn := Def.taskDyn {
      tmpfsDirectoryMode.value match {
        case TmpfsDirectoryMode.Symlink => tmpfsLink
        case TmpfsDirectoryMode.Mount => tmpfsMount
      }
    }.value,
    (initialize in Compile) := {
      implicit val logger: Logger = sLog.value
      logger.debug("[SbtTmpfsPlugin] try to clean dead symlinks.")
      LinkTool.cleanDeadLinks(target.value.listFiles()) //clean possible different cross version.
      LinkTool.cleanDeadLinks(tmpfsLinkDirectories.value)
      LinkTool.cleanDeadLinks(tmpfsMappingDirectories.value.values.flatten.toSeq)
      (initialize in Compile).value
    }
  )

  private val taskDependentRelationships = Seq(
    tmpfsLink := (tmpfsLink runBefore update).value,
    tmpfsLink := (tmpfsLink triggeredBy clean).value,
    tmpfsSyncMapping := (tmpfsSyncMapping triggeredBy(tmpfsOn, tmpfsLink, tmpfsMount)).value
  )

  override def trigger: PluginTrigger = allRequirements
  override val projectSettings: Seq[Def.Setting[_]] =
    defaultSbtTmpfsSettings ++ taskDefinition ++ taskDependentRelationships

  private def isCi: Boolean = sys.env.contains("CI")
}
