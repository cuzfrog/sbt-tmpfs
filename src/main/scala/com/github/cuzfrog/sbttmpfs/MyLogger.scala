package com.github.cuzfrog.sbttmpfs

import sbt.{Level, Logger}

private class MyLogger(logger: Logger) extends Logger {
  override def ansiCodesSupported: Boolean = logger.ansiCodesSupported

  private final val prefix = "[SbtTmpfsPlugin] "
  override def trace(t: => Throwable): Unit = logger.trace(t)
  override def success(message: => String): Unit = logger.success(prefix + message)
  override def log(level: Level.Value,
                   message: => String): Unit = logger.log(level, prefix + message)
}
