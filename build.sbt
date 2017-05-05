import sbt.Keys._
import Settings._

shellPrompt in ThisBuild := { state => Project.extract(state).currentRef.project + "> " }
sbtPlugin := true

lazy val root = (project in file("."))
  .settings(commonSettings, publicationSettings, readmeVersionSettings)
  .settings(
    name := "sbt-tmpfs",
    version := "0.0.1-SNAPSHOT",
    libraryDependencies ++= Seq(

    ),
    reColors := Seq("magenta")
  )