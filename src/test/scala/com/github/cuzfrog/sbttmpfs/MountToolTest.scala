package com.github.cuzfrog.sbttmpfs

import java.io.File

import utest._
import ExMethod._

object MountToolTest extends TestSuite {

  private implicit val logger: TestLogger = new TestLogger
  private val tmpDir: File = new File("/tmp")

  val tests: Tests = if (!tmpDir.isOfTmpfs) {
    logger.warn("/tmp is not of tmpfs, skip MountToolTest")
    Tests {} //ignore
  } else Tests {
    "mount-already-tmpfs" - {
      val msg = MountTool.mountOne(tmpDir, s"sudo mount ${tmpDir.getAbsolutePath}")
      assert(msg == s"$tmpDir is already of tmpfs, abort mounting.")
    }
  }
}
