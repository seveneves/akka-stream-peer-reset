package seven.akka.stream.peer

import java.io.InputStream
import java.net.InetAddress

import akka.stream.io.InputStreamSource
import akka.stream.scaladsl.Tcp.{OutgoingConnection, ServerBinding}
import akka.stream.scaladsl._
import akka.util.ByteString
import grizzled.slf4j.Logging
import org.reactivestreams.{Subscriber, Subscription}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, FunSpecLike, Matchers}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future, Promise}
import scala.util.Random

class PeerCloseSpec extends FunSpecLike with BeforeAndAfterAll with FlowSpecSupport with ScalaFutures with Matchers {


  implicit val config = PatienceConfig(timeout = Span(30, Seconds))

  describe("Stream") {
    val streamSize = 10 * 1024 * 1024 + 1232
    val clientNumber = 300

    it("should support multiple uploads and have same byte sizes on the downstream end") {
      import scala.concurrent.ExecutionContext.Implicits.global
      val allResults = 0 until clientNumber map (_ => Promise[Long]())
      val serverBinding = startServer(allResults)
      def mockedFileInputStream = new GeneratingInputStream(1, streamSize)
      0 until clientNumber foreach { i=>
        val starter = Promise[Any]()
        startClient(serverBinding, starter.future, mockedFileInputStream)
        println(s"Kicking $i")
        starter.success("")
      }

      whenReady(Future.sequence(allResults.map(_.future))) { all =>
        all.foreach(_ shouldEqual streamSize)
      }
    }
  }

  def startServer(promises: Seq[Promise[Long]]): Tcp.ServerBinding = {
    val port = Random.nextInt(1000) + 35823
    var index = 0

    val futureBinding = Tcp().bind(InetAddress.getLoopbackAddress.getHostAddress, port).to(Sink.foreach { connection =>
      Source.empty via connection.flow runWith sizeSink(promises(index))
      index += 1
    }).run()

    Await.result(futureBinding, 3.seconds)
  }

  def sizeSink(promise: Promise[Long]) = Sink(new Subscriber[ByteString] with Logging {
    private var subscription: Subscription = _

    var size = 0L

    override def onSubscribe(s: Subscription): Unit = {
      subscription = s
      subscription.request(1)
    }

    override def onError(t: Throwable): Unit = {
      logger.error(s"Closing Subscriber because of error", t)
      promise.tryFailure(t)
    }

    override def onComplete(): Unit = {
      debug(s"closing Subscriber on complete")
      promise.trySuccess(size)
    }

    override def onNext(data: ByteString): Unit = {
      size += data.length
      subscription.request(1)
    }
  })

  def startClient(binding: ServerBinding, startOn: Future[Any], in: InputStream) = {

    val outgoingConnection: Flow[ByteString, ByteString, Future[OutgoingConnection]] = Tcp().outgoingConnection(binding.localAddress)

    clientSource(startOn, in).via(outgoingConnection).runWith(Sink.ignore)
  }

  def clientSource(startOn: Future[Any], inputStream: InputStream): Source[ByteString, _] = Source(startOn)
    .map(_ => InputStreamSource(() => inputStream))
    .flatten(FlattenStrategy.concat)

}
