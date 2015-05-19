package seven.akka.stream.peer

import java.io.InputStream

import grizzled.slf4j.Logging

class GeneratingInputStream(toReplicate: Array[Byte], size: Long) extends InputStream with Logging {
  var index: Long = 0
  val replicateSize = toReplicate.length

  def resetToHead() = index = 0

  override def read(): Int = index match {
    case i if i >= size => -1
    case _ =>
      val i = (index % replicateSize).toInt
      index += 1L
      toReplicate(i)
  }

  override def close(): Unit = {
    info("GeneratingInputStream got closed")
    super.close()
  }
}
