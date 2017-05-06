logLevel in ThisBuild := Level.Debug

lazy val testproject = (project in file("."))
  .settings(
    version := "0.1",
    scalaVersion := "2.12.2"
  ).enablePlugins(SbtTmpfsPlugin)