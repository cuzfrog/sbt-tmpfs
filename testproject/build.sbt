
logLevel in tmpfsOn := Level.Debug
logLevel in compile := Level.Info

version in ThisBuild := "0.1"
scalaVersion in ThisBuild := "2.12.2"

lazy val testproject = (project in file("."))
  .settings(
    tmpfsDirectoryMode := TmpfsDirectoryMode.Mount,
    tmpfsMountSizeLimit := 255,
    tmpfsMappingDirectories := Map(
      baseDirectory.value / "preservedSource" -> target.value / "preserved"
    )
  )


lazy val subproject = (project in file("./subproject"))
  .settings(

  )

