package com.github.cuzfrog

import java.io.File
import java.nio.file.{Files, Path, StandardOpenOption}

import scala.annotation.tailrec
import scala.util.Random

/**
  * Compare under different mount of files, which of `rsync`, `sbt.IO.copy` is better at dir synchronization.
  *
  * sbt.IO.copyDirectories provides fairly smart implementation of files copying.
  * Modern `rsync` brings good performance.
  *
  */
object RsyncVsSbtCopyTest {

  val originalDirs = Seq(
    TestDirGrp("small", new File("/tmp/small"), 100, 10, 3),
    TestDirGrp("medium", new File("/tmp/medium"), 1000, 100, 6),
    TestDirGrp("big", new File("/tmp/big"), 10000, 100, 12),
    TestDirGrp("big2", new File("/tmp/big2"), 1000, 1000, 12)
  ).map(FileGen.populateDir)

  val ramDirs = originalDirs.map { grp =>
    val dir = new File("/tmp/t1/" + grp.name)
    if (dir.exists) sbt.IO.delete(dir)
    sbt.IO.copyDirectory(grp.dir, dir)
    grp.copy(dir = dir, name = grp.name + "-ram")
  }

  val ssdDirs = originalDirs.filter(g => !g.name.contains("big")).map { grp =>
    val dir = new File("/home/cuz/t1/" + grp.name)
    if (dir.exists) sbt.IO.delete(dir)
    sbt.IO.copyDirectory(grp.dir, dir)
    grp.copy(dir = dir, name = grp.name + "-ssd")
  }

  private def rsync(src: File, dest: File): Unit = {
    sbt.Process(s"rsync -a ${src.getAbsolutePath}/ ${dest.getAbsolutePath}/").!
  }

  private def cp(src: File, dest: File): Unit = {
    sbt.Process(s"""cp -au ${src.getAbsolutePath}/. ${dest.getAbsolutePath}""").!
  }


  def main(args: Array[String]): Unit = {
    val measures = ssdDirs.map { grp =>
      args.headOption match {
        case Some("sbt") => syncTestSbt(grp, 10, sbt.IO.copyDirectory(_, _), "sbt")
        case Some("rsync") => syncTestSbt(grp, 10, rsync, "rsync")
        case Some("cp") => syncTestSbt(grp, 10, cp, "cp")
        case _ => ???
      }
    }
    println("Result:")
    print(s"| ${args.head} |")
    measures.foreach { r =>
      print(s" ${r.totalTimeElapsed}ms |")
    }
    println()
    println("No-modification:")
    print(s"| ${args.head} |")
    measures.foreach { r =>
      print(s" ${r.totalTimeElapsedNoModifi}ms |")
    }
  }

  /** Return (time-cost,time-cost-no-modification) */
  private def syncTestSbt(grp: TestDirGrp, times: Int,
                          testFunc: (File, File) => Unit, functionName: String,
                          warmTimes: Int = 3): TestResult = {
    val dest = sbt.IO.createTemporaryDirectory
    sbt.IO.copyDirectory(grp.dir, dest)
    def updateDir() = FileGen.updateDir(grp.dir.toPath, grp.fileTotalCount / 20, grp.maxFileSizeInKB, grp.fileTotalCount / 20)
    println("-----------------------------")
    println(s"Test($functionName|${grp.name}) warms up:")
    (1 to warmTimes).foreach { i =>
      updateDir()
      testFunc(grp.dir, dest)
      print(s"..$i")
    }
    println()
    println(s"Test($functionName|${grp.name}) begins:")

    val totalTimeElapsed = (1 to times).map { i =>
      updateDir()
      val time1 = System.currentTimeMillis()
      testFunc(grp.dir, dest)
      val time2 = System.currentTimeMillis()
      val timeElapsed = time2 - time1
      print(s".$i")
      timeElapsed
    }.sum
    println()
    println(s"Test($functionName|${grp.name}): total rounds:$times, total time elapsed: $totalTimeElapsed ms")
    println()
    println(s"Test($functionName|${grp.name}|No modification) begins:")
    val totalTimeElapsedNoModifi = (1 to times).map { i =>
      val time1 = System.currentTimeMillis()
      testFunc(grp.dir, dest)
      val time2 = System.currentTimeMillis()
      val timeElapsed = time2 - time1
      print(s".$i")
      timeElapsed
    }.sum
    println()
    println(s"Test($functionName|${grp.name}|No modification): total rounds:$times," +
      s" total time elapsed: $totalTimeElapsedNoModifi ms")
    sbt.IO.delete(dest)

    TestResult(grp, totalTimeElapsed, totalTimeElapsedNoModifi)
  }
}

case class TestDirGrp(name: String, dir: File, fileTotalCount: Int, maxFileSizeInKB: Int, maxDirDepth: Int)
case class TestResult(grp: TestDirGrp, totalTimeElapsed: Long, totalTimeElapsedNoModifi: Long)

private object FileGen {
  /** Given a dir, populate the dir with specified random files and return it. */
  def populateDir(testDirGrp: TestDirGrp): TestDirGrp = {
    import testDirGrp._
    val ratioPercentage: Int = 50
    if (!dir.exists) sbt.IO.createDirectory(dir)
    require(dir.isDirectory, "Dir to populate is not a directory.")
    require(fileTotalCount * maxFileSizeInKB <= 1024 * 1024, "Potentially generate too large directory.")
    require(maxDirDepth <= 16, "Too large dir depth.")
    if (dir.listFiles().nonEmpty) {
      println("Dir already populated.")
      return testDirGrp
    }

    @tailrec def ramify(currentLevel: Int, fileCount: Int, parent: Path): Unit = {
      if (fileCount >= fileTotalCount) return

      if (Random.nextInt(100) < ratioPercentage) {
        val fileName = Random.alphanumeric.take(8).mkString + fileCount
        val file = Files.createFile(parent.resolve(fileName))
        val dataStream = Random.alphanumeric.take(Random.nextInt(maxFileSizeInKB) * 1024 + 1)
        Files.write(file, dataStream.map(_.toByte).toArray)
        //println(fileCount + "|" + fileName + "|" + dataStream.size)
        ramify(currentLevel, fileCount + 1, parent) //create file and stay
      } else {
        if (Random.nextBoolean && currentLevel < maxDirDepth) {
          val folderName = Random.alphanumeric.take(8).mkString
          val newFolder = parent.resolve(folderName)
          sbt.IO.createDirectory(newFolder.toFile)
          ramify(currentLevel + 1, fileCount, newFolder) //go deeper
        } else if (currentLevel > 1) {
          ramify(currentLevel - 1, fileCount, parent.getParent) //go shallower
        } else {
          ramify(currentLevel, fileCount, parent) //stay
        }
      }
    }

    ramify(1, 0, dir.toPath)
    testDirGrp
  }

  def updateDir(dir: Path, fileToAddTotalCount: Int, maxFileSizeInKB: Int, fileToModifyTotalCount: Int): Unit = {
    @tailrec def ramify(fileToAddCount: Int, fileToModifyCount: Int, parent: File): Unit = {
      if (fileToAddCount >= fileToAddTotalCount && fileToModifyCount >= fileToModifyTotalCount) return

      if (Random.nextBoolean()) { //move
        val subDirOpt = parent.listFiles.find(_.isDirectory)
        if (Random.nextBoolean && subDirOpt.isDefined) {
          ramify(fileToAddCount, fileToModifyCount, subDirOpt.get) //go deeper
        } else if (!Files.isSameFile(parent.toPath, dir)) {
          ramify(fileToAddCount, fileToModifyCount, parent.getParentFile) //go shallower
        } else ramify(fileToAddCount, fileToModifyCount, parent) //stay
      }
      else { //add or modify
        if (Random.nextBoolean() && fileToAddCount < fileToAddTotalCount) { //add new file
          val fileName = Random.alphanumeric.take(8).mkString + fileToAddCount
          val file = Files.createFile(parent.toPath.resolve(fileName))
          val dataStream = Random.alphanumeric.take(Random.nextInt(maxFileSizeInKB) * 1024 + 1)
          Files.write(file, dataStream.map(_.toByte).toArray)
          ramify(fileToAddCount + 1, fileToModifyCount, parent)
        } else if (fileToModifyCount < fileToModifyTotalCount) { //modify existing file
          val fileList = parent.listFiles.filter(_.isFile)
          val size = fileList.length
          val modifiedCount = if (size > 0) {
            val file = fileList(Random.nextInt(size))
            val dataStream = Random.alphanumeric.take(Random.nextInt(maxFileSizeInKB) * 1024 + 1)
            Files.write(file.toPath, dataStream.map(_.toByte).toArray, StandardOpenOption.APPEND)
            fileToModifyCount + 1
          } else fileToModifyCount
          ramify(fileToAddCount, modifiedCount, parent)
        } else {
          ramify(fileToAddCount, fileToModifyCount, parent) //stay do nothing
        }
      }
    }

    ramify(0, 0, dir.toFile)
  }
}
