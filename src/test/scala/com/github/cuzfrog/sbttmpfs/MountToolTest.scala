package com.github.cuzfrog.sbttmpfs

import java.io.File

import org.junit._
import org.junit.Assert._
import org.hamcrest.CoreMatchers._

import ExMethod._

class MountToolTest {

  private implicit val logger: TestLogger = new TestLogger
  private val tmpDir: File = new File("/tmp")

  @Test
  def mount_already_tmpfs(): Unit = {
    if (!tmpDir.isOfTmpfs) {
      logger.warn("/tmp is not of tmpfs, skip MountToolTest")
    } else {
      val msg = MountTool.mountOne(tmpDir, s"sudo mount ${tmpDir.getAbsolutePath}")
      assert(msg == s"$tmpDir is already of tmpfs, abort mounting.")
    }
  }


}
