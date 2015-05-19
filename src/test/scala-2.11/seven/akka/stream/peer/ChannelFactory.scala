package seven.akka.stream.peer

import java.nio.ByteBuffer
import java.nio.channels.WritableByteChannel
import java.nio.file.Path

import grizzled.slf4j.Logging
import seven.akka.stream.peer.DigesterStage.{Digester, Digest}

import scala.concurrent.Promise

trait ChannelFactory {

  def createFileChannel(path: Path): WritableByteChannel

}


object ChannelFactory {
  def digestChannel = new DigestFileChannel()

  class DigestFileChannel extends ChannelFactory with Logging {

    private val result = Promise[Digest]()
    private val digester = new Digester()

    info("Creating DigestFileChannel")

    override def createFileChannel(path: Path): WritableByteChannel = new WritableByteChannel {
      info("Creating WritableByteChannel")

      override def write(src: ByteBuffer): Int = {

        try {
          val remaining = src.remaining()
          (0 until remaining).foreach { i =>
            val byte = src.get()
            digester.update(Array(byte))
          }
          remaining
        } catch {
          case t: Throwable =>
            error("On writing", t)
            throw t
        }
      }

      override def isOpen: Boolean = true

      override def close(): Unit = {
        info("DigestFileChannel got closed")

        result.success(digester.digest())
      }
    }

    def digest = result.future
  }

}