import sbt.Keys._
import Settings._

shellPrompt in ThisBuild := { state => Project.extract(state).currentRef.project + "> " }
sbtPlugin := true

lazy val root = (project in file("."))
  .settings(commonSettings, publicationSettings, readmeVersionSettings)
  .settings(
    name := "sbt-tmpfs",
    description := "sbt plugin to speed up development by leveraging tmpfs.",
    //version := "0.1.0-SNAPSHOT",
    libraryDependencies ++= Seq(

    ),
    reColors := Seq("magenta")
  )