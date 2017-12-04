package com.github.cuzfrog.sbttmpfs

import com.github.cuzfrog.sbttmpfs.support.ThreadSafe
import sbt.{Level, Logger}

import scala.collection.mutable.ArrayBuffer

@ThreadSafe
private final class TestLogger extends Logger {
  private val buffer: ArrayBuffer[String] = ArrayBuffer.empty

  override def trace(t: => Throwable): Unit = buffer.synchronized {
    val stackTrace = t.getStackTrace.mkString(System.lineSeparator())
    buffer += ("[trace]" + stackTrace)
  }
  override def success(message: => String): Unit = buffer.synchronized {
    buffer += ("[success] " + message)
  }
  override def log(level: Level.Value, message: => String): Unit = buffer.synchronized {
    buffer += s"[$level] $message"
  }

  def flush(): Unit = buffer.synchronized {
    buffer.foreach(println)
    buffer.clear()
  }

  def clearBuffer(): Unit = buffer.synchronized {buffer.clear()}
  def getBufferContent: Seq[String] = buffer.clone()
}
