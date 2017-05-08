
logLevel in tmpfsLink := Level.Debug
logLevel in tmpfsMount := Level.Debug
logLevel in tmpfsOn := Level.Debug
logLevel in tmpfsSyncMapping := Level.Debug
logLevel in compile := Level.Info
logLevel in initialize := Level.Debug

version in ThisBuild := "0.1"
scalaVersion in ThisBuild := "2.12.2"

lazy val testproject = (project in file("."))
  .settings(
    tmpfsDirectoryMode := TmpfsDirectoryMode.Symlink,
    tmpfsMountSizeLimit := 255,
    tmpfsMappingDirectories := Map(
      baseDirectory.value / "preservedSource" -> target.value / "preserved"
    )
  )


lazy val subproject = (project in file("./subproject"))
  .settings(

  )

