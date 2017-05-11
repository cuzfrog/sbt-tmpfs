import sbt.Keys._
import Settings._

shellPrompt in ThisBuild := { state => Project.extract(state).currentRef.project + "> " }
sbtPlugin := true

lazy val root = (project in file("."))
  .settings(commonSettings, publicationSettings)
  .settings(
    name := "sbt-tmpfs",
    description := "sbt plugin to speed up development by leveraging tmpfs.",
    //version := "0.2.1-SNAPSHOT",
    libraryDependencies ++= Seq(

    ),
    reColors := Seq("magenta")
  )

addCompilerPlugin("org.psywerx.hairyfotr" %% "linter" % "0.1.17")