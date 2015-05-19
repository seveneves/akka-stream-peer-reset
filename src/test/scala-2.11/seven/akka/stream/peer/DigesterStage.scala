package seven.akka.stream.peer

import java.security.MessageDigest
import javax.xml.bind.annotation.adapters.HexBinaryAdapter

import akka.stream.scaladsl.Flow
import akka.stream.stage.{Context, PushStage, SyncDirective, TerminationDirective}
import akka.util.ByteString

object DigesterStage {

  case class Digest(checksum: String, size: Long)
  
  class Digester {
    private val adapter = new HexBinaryAdapter()
    private val digester = MessageDigest.getInstance("SHA-1")
    var size = 0L

    def update(bytes: Array[Byte]): Unit = {
      size += bytes.length
      digester.update(bytes)
    }
    
    def digest(): Digest = Digest(adapter.marshal(digester.digest()), size)
  }
}

