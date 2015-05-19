package seven.akka.stream.peer

import java.io.{File, InputStream}
import java.net.InetAddress

import akka.stream.scaladsl.Tcp.{OutgoingConnection, ServerBinding}
import akka.stream.scaladsl.{Flow, Sink, Source, Tcp}
import akka.util.ByteString
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, FunSpecLike, Matchers}
import seven.akka.stream.peer.DigesterStage.Digest

import scala.concurrent.duration._
import scala.concurrent.{Await, Future, Promise}
import scala.util.Random

class PeerCloseSpec extends FunSpecLike with BeforeAndAfterAll with FlowSpecSupport with ScalaFutures with Matchers {


  implicit val config = PatienceConfig(timeout = Span(10, Seconds))

  describe("File stream") {

    it("should upload multiple files and have same byte sizes and checksums on the other end") {
      import scala.concurrent.ExecutionContext.Implicits.global
      val fileSize = 10 * 1024 * 1024 + 1232
      val clientNumber = 100
      val channels = 0 until clientNumber map (_ => ChannelFactory.digestChannel)
      val serverBinding = startServer(channels)
      def mockedFileInputStream = new GeneratingInputStream(Array(113, 121, 56, 45, 72).map(_.toByte), fileSize)
      val results = 0 until clientNumber map { i=>
        val starter = Promise[Any]()
        startClient(serverBinding, starter.future, mockedFileInputStream)
        info(s"Kicking $i")
        starter.success("")
        channels(i).digest
      }

      whenReady(Future.sequence(results)) { all =>
        all.foreach(_ shouldEqual Digest("15B3DDCB524346D6742D118A3F4C0ED904ADC01B", fileSize))
      }
    }
  }

  def startServer(digestChannel: Seq[ChannelFactory]): Tcp.ServerBinding = {
    def serverSink(i: Int) = Sink(Flow[ByteString]) { implicit b =>
      inbound => {
        import akka.stream.scaladsl.FlowGraph.Implicits._
        inbound.outlet ~> WriteToFile.sink(new File("").toPath, digestChannel(i))
        inbound.inlet
      }
    }

    val port = Random.nextInt(1000) + 35823
    var index = 0

    val futureBinding = Tcp().bind(InetAddress.getLoopbackAddress.getHostAddress, port).to(Sink.foreach { connection =>
      Source.empty via connection.flow runWith serverSink(index)
      index += 1
    }).run()

    Await.result(futureBinding, 3.seconds)
  }

  def startClient(binding: ServerBinding, startOn: Future[Any], in: InputStream) = {

    val outgoingConnection: Flow[ByteString, ByteString, Future[OutgoingConnection]] = Tcp().outgoingConnection(binding.localAddress)

    UploadStage.source(startOn, in).via(outgoingConnection).runWith(Sink.ignore)
  }

}
