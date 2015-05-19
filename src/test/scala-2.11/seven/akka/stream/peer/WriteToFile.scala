package seven.akka.stream.peer

import java.nio.file.Path

import akka.stream.scaladsl.Sink
import akka.util.ByteString
import grizzled.slf4j.Logging
import org.reactivestreams.{Subscriber, Subscription}

object WriteToFile {

  def sink(filePath: Path, writer: ChannelFactory) = Sink(new ByteStringWriter(filePath, writer))

  private class ByteStringWriter(filePath: Path, writer: ChannelFactory) extends Subscriber[ByteString] with Logging {
    private var subscription: Subscription = _

    val channel = writer.createFileChannel(filePath)

    override def onSubscribe(s: Subscription): Unit = {
      subscription = s
      subscription.request(1)
    }

    override def onError(t: Throwable): Unit = {
      logger.error(s"Closing file channel $filePath because of error", t)
      close()
    }

    override def onComplete(): Unit = {
      trace(s"byteStringWriter onComplete $filePath")
      close()
    }

    override def onNext(data: ByteString): Unit = {
      trace(s"writing ${data.length} bytes to $filePath")
      channel.write(data.asByteBuffer)
      subscription.request(1)
    }

    def close(): Unit = {
      if (channel.isOpen) {
        channel.close()
      }
    }
  }
}
