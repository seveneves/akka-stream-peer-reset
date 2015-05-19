package seven.akka.stream.peer

import java.io.InputStream

import grizzled.slf4j.Logging

class GeneratingInputStream(toReplicate: Byte, size: Long) extends InputStream with Logging {
  var index: Long = 0

  def resetToHead() = index = 0

  override def read(): Int = index match {
    case i if i >= size =>
      -1
    case _ =>
      index += 1
      toReplicate
  }

  override def close(): Unit = {
    info("GeneratingInputStream got closed")
    super.close()
  }
}
