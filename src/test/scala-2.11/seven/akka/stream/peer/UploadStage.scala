package seven.akka.stream.peer

import java.io.InputStream

import akka.stream.io.InputStreamSource
import akka.stream.scaladsl.{FlattenStrategy, Source}
import akka.util.ByteString
import grizzled.slf4j.Logging

import scala.concurrent.Future

object UploadStage extends Logging {

  def source(startOn: Future[Any], inputStream: InputStream): Source[ByteString, _] = Source() { implicit builder =>
    import akka.stream.scaladsl.FlowGraph.Implicits._
    val bytes = builder.add(Source(startOn).map(_ => InputStreamSource(() => inputStream)).flatten(FlattenStrategy.concat))
    bytes.outlet
  }
}
