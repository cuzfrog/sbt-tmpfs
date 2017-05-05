import sbt.TaskKey

object MyTasks {
  val generateBat: TaskKey[Unit] = TaskKey[Unit]("generate-bat", "Generate a bat file for window shell.")
  val copyApp: TaskKey[Unit] = TaskKey[Unit]("copy-app", "Copy app files to target.")
  val cleanAll: TaskKey[Unit] = TaskKey[Unit]("clean-all", "Clean all files in target folders.")
  val versionReadme: TaskKey[Unit] = TaskKey[Unit]("version-readme", "Update version in README.MD")
}