import java.nio.file.Paths

import sbt.Keys.{version, _}
import sbt._
import MyTasks._
import bintray.BintrayPlugin.autoImport._


object Settings {

  val commonSettings = Seq(
    resolvers += Resolver.bintrayRepo("cuzfrog", "maven"),
    organization := "com.github.cuzfrog",
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
    licenses += ("Apache-2.0", url("https://opensource.org/licenses/Apache-2.0")),
    cleanSnapshot := {
      val home = System.getenv("HOME")
      IO.delete(new File(home) / ".ivy2/local/com.github.cuzfrog/sbt-tmpfs")
    },
    cleanSnapshot := (cleanSnapshot runBefore publishLocal).value
  )

  val publicationSettings = Seq(
    //publishTo := Some("My Bintray" at s"https://api.bintray.com/maven/cuzfrog/maven/${name.value}/;publish=1"),
    publishMavenStyle := false,
    bintrayRepository := "sbt-plugins",
    bintrayOrganization in bintray := None,
    generateCredential := {
      val home = System.getenv("HOME")
      val bintrayUser = System.getenv("BINTRAY_USER")
      val bintrayPass = System.getenv("BINTRAY_PASS")
      val content = Seq(
        "realm = Bintray API Realm",
        "host = api.bintray.com",
        "user = " + bintrayUser,
        "password = " + bintrayPass
      )
      IO.writeLines(Paths.get(home, ".bintray", ".credentials").toFile, content)
    }
  )

  val readmeVersionSettings = Seq(
    (compile in Compile) := ((compile in Compile) dependsOn versionReadme).value,
    versionReadme := {
      val contents = IO.read(file("README.md"))
      val regex =raw"""(?<=addSbtPlugin\("com\.github\.cuzfrog" % "${name.value}" % ")[\+\d\w\-\.]+(?="\))"""
      val releaseVersion = version.value.split("""\+""").head
      val newContents = contents.replaceAll(regex, releaseVersion)
      IO.write(file("README.md"), newContents)
      streams.value.log.info(s"Version set to $releaseVersion")
    }
  )
}