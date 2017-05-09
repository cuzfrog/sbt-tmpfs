import sbt._
import sbt.Keys._

object Settings {
  val taskA = taskKey[Unit]("test task A")
  val taskB = taskKey[Unit]("test task B")
  val taskC = taskKey[Unit]("test task B")

  val commonSettings = Seq(
    taskA := {
      println(s"Task A runBefore compile in project ${name.value}")
    },
    taskB := {
      println(s"Task B triggerBy compile in project ${name.value}")
    },
    taskC := {
      println(s"compile dependsOn taskC in project ${name.value}")
    },
    taskA := (taskA runBefore update).value
//    taskB := (taskB triggeredBy compile.in(Compile)).value,
//    compile.in(Compile) := (compile.in(Compile) dependsOn taskC).value
  )
}