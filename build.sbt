import Settings._

shellPrompt in ThisBuild := { state => Project.extract(state).currentRef.project + "> " }
sbtPlugin := true
crossSbtVersions := Vector("0.13.16", "1.1.0")

lazy val root = (project in file("."))
  .settings(commonSettings ++ publicationSettings: _*)
  .settings(
    name := "sbt-tmpfs",
    description := "sbt plugin to speed up development by leveraging tmpfs.",
    //version := "0.3.3-SNAPSHOT",
    libraryDependencies ++= Seq(

    )
  )

//addCompilerPlugin("org.psywerx.hairyfotr" %% "linter" % "0.1.17")
//crossBuildingSettings
