import sbt.Keys._
import Settings._

shellPrompt in ThisBuild := { state => Project.extract(state).currentRef.project + "> " }
sbtPlugin := true

lazy val root = (project in file("."))
  .settings(commonSettings ++ publicationSettings: _*)
  .settings(
    name := "sbt-tmpfs",
    description := "sbt plugin to speed up development by leveraging tmpfs.",
    //version := "0.3.0-SNAPSHOT",
    //CrossBuilding.crossSbtVersions := List("0.13", "1.0"),
    libraryDependencies ++= Seq(

    )
  )

//addCompilerPlugin("org.psywerx.hairyfotr" %% "linter" % "0.1.17")
//crossBuildingSettings