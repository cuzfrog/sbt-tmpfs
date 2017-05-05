import sbt.Keys._
import sbt._
import MyTasks._

object Settings {

  val commonSettings = Seq(
    resolvers ++= Seq(
      Resolver.bintrayRepo("cuzfrog", "maven")
    ),
    organization := "com.github.cuzfrog",
    scalaVersion := "2.12.2",
    crossScalaVersions := Seq("2.11.11", "2.12.2"),
    scalacOptions ++= Seq(
      //"-Xlint",
      "-unchecked",
      "-deprecation",
      "-feature",
      "-language:postfixOps",
      "-language:implicitConversions",
      "-language:higherKinds",
      "-language:existentials"),
    libraryDependencies ++= Seq(
      "junit" % "junit" % "4.12" % "test",
      "com.novocode" % "junit-interface" % "0.11" % "test->default"
    ),
    logBuffered in Test := false,
    testOptions += Tests.Argument(TestFrameworks.JUnit, "-v", "-q", "-a"),
    parallelExecution in Test := false,
    licenses += ("Apache-2.0", url("https://opensource.org/licenses/Apache-2.0"))
  )

  val publicationSettings = Seq(
    publishTo := Some("My Bintray" at s"https://api.bintray.com/maven/cuzfrog/maven/${name.value}/;publish=1")
  )

  val readmeVersionSettings = Seq(
    (compile in Compile) := ((compile in Compile) dependsOn versionReadme).value,
    versionReadme := {
      val contents = IO.read(file("README.md"))
      val regex =raw"""(?<=libraryDependencies \+= "com\.github\.cuzfrog" %% "${name.value}" % ")[\d\w\-\.]+(?=")"""
      val newContents = contents.replaceAll(regex, version.value)
      IO.write(file("README.md"), newContents)
    }
  )
}