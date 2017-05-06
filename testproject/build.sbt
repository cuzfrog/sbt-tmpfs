
logLevel in ThisBuild := Level.Debug


version in ThisBuild := "0.1"
scalaVersion in ThisBuild := "2.12.2"


lazy val testproject = (project in file("."))


lazy val subproject = (project in file("./subproject"))