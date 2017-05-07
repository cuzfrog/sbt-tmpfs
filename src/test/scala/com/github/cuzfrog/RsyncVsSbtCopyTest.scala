package com.github.cuzfrog

import java.io.File
import java.nio.file.{Files, Path, Paths}

import scala.util.Random

/**
  * Compare under different mount of files, which of `rsync`, `sbt.IO.copy` is better at dir synchronization.
  *
  * sbt.IO.copyDirectories provides fairly smart implementation of files copying.
  * Modern `rsync` brings good performance.
  *
  */
object RsyncVsSbtCopyTest extends App {

}

object CreateTestDirs extends App {
  FileHelper.populateDir(
    dir = sbt.IO.createTemporaryDirectory.toPath,
    fileTotalCount = 100,
    maxFileSizeInKB = 2,
    maxDirDepth = 3)
}

private object FileHelper {
  /** Given a dir, populate the dir with specified random files and return it. */
  def populateDir(dir: Path, fileTotalCount: Int, maxFileSizeInKB: Int, maxDirDepth: Int,
                  ratioPercentage: Int = 50): Path = {
    require(dir.toFile.isDirectory, "Dir to populate is not a directory.")
    require(fileTotalCount * maxFileSizeInKB <= 1024 * 1024, "Potentially generate too large directory.")
    require(maxDirDepth <= 16, "Too large dir depth.")

    def ramify(currentLevel: Int, fileCount: Int, parent: Path): Unit = {
      if (fileCount >= fileTotalCount) return

      if (Random.nextInt(100) < ratioPercentage) {
        val fileName = Random.alphanumeric.take(8).mkString + fileCount
        val file = Files.createFile(parent.resolve(fileName))
        val dataStream = Random.alphanumeric.take(maxFileSizeInKB * 1024)
        Files.write(file, dataStream.map(_.toByte).toArray)
        ramify(currentLevel, fileCount + 1, parent) //create file and stay
      } else {
        if (Random.nextBoolean && currentLevel <= maxDirDepth) {
          val folderName = Random.alphanumeric.take(8).mkString
          ramify(currentLevel + 1, fileCount, parent.resolve(folderName)) //go deeper
        } else if (currentLevel >= 1) {
          ramify(currentLevel - 1, fileCount, parent.getParent) //go shallower
        } else {
          ramify(currentLevel, fileCount, parent) //stay
        }
      }
    }

    ramify(1, 0, dir)
    dir
  }
}
