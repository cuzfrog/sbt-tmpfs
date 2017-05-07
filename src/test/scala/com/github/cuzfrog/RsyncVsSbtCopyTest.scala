package com.github.cuzfrog

import java.io.File
import java.nio.file.{Files, Path, Paths, StandardOpenOption}

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

  private val testDirs = Seq(
    TestDirGrp("small", sbt.IO.createTemporaryDirectory, 100, 10, 3),
    TestDirGrp("medium", sbt.IO.createTemporaryDirectory, 3000, 100, 6),
    TestDirGrp("big", sbt.IO.createTemporaryDirectory, 50000, 200, 12)
  ).map { grp =>
    FileGen.populateDir(grp.dir.toPath, grp.fileCnt, grp.maxFileSizeInKB, grp.maxDirDepth)
    grp
  }

  private def rsync(src: File, dest: File): Unit = {
    sbt.Process(s"rsync -a ${src.getAbsolutePath}/ ${dest.getAbsolutePath}/")
  }

  def main(args: Array[String]): Unit = args.headOption match {
    case Some("sbt") => testDirs.foreach { grp =>
      syncTestSbt(grp, 5, sbt.IO.copyDirectory(_, _), "sbt")
    }
    case Some("rsync") => testDirs.foreach { grp =>
      syncTestSbt(grp, 5, rsync, "rsync")
    }
    case _ => println("Bad args.")
  }

  private def syncTestSbt(grp: TestDirGrp, times: Int,
                          testFunc: (File, File) => Unit, functionName: String,
                          warmTimes: Int = 5) = {
    val dest = sbt.IO.createTemporaryDirectory
    sbt.IO.copyDirectory(grp.dir, dest)
    def updateDir() = FileGen.updateDir(grp.dir.toPath, grp.fileCnt / 100, grp.maxFileSizeInKB / 10, grp.fileCnt / 100)

    println(s"Test(${grp.name}) warms up:")
    (1 to warmTimes).foreach { i =>
      updateDir()
      testFunc(grp.dir, dest)
      print(s"..$i")
    }
    println()
    println(s"Test(${grp.name}) begins:")

    val totalTimeElapsed = (1 to times).map { i =>
      updateDir()
      val time1 = System.currentTimeMillis()
      testFunc(grp.dir, dest)
      val time2 = System.currentTimeMillis()
      val timeElapsed = time2 - time1
      println(s"Round-$i time elapsed: $timeElapsed")
      timeElapsed
    }.sum

    println(s"Test(${grp.name}): total rounds:$times, total time elapsed: $totalTimeElapsed")
  }
}

case class TestDirGrp(name: String, dir: File, fileCnt: Int, maxFileSizeInKB: Int, maxDirDepth: Int)

object CreateTestDirsOnDisk extends App {
  val dir = FileGen.populateDir(
    dir = sbt.IO.createTemporaryDirectory.toPath,
    fileTotalCount = 100,
    maxFileSizeInKB = 2,
    maxDirDepth = 3)

  println(dir)
}

private object FileGen {
  /** Given a dir, populate the dir with specified random files and return it. */
  def populateDir(dir: Path, fileTotalCount: Int, maxFileSizeInKB: Int, maxDirDepth: Int,
                  ratioPercentage: Int = 50): Path = {
    require(dir.toFile.isDirectory, "Dir to populate is not a directory.")
    require(fileTotalCount * maxFileSizeInKB <= 1024 * 1024, "Potentially generate too large directory.")
    require(maxDirDepth <= 16, "Too large dir depth.")

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

    ramify(1, 0, dir)
    dir
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
          val fileList = parent.listFiles
          val size = fileList.size
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
