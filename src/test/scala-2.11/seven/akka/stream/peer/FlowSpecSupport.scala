package seven.akka.stream.peer

import akka.actor.ActorSystem
import akka.stream.{ActorFlowMaterializer, ActorFlowMaterializerSettings}
import com.typesafe.config.ConfigFactory
import org.scalatest.{Suite, BeforeAndAfterAll}

trait FlowSpecSupport extends BeforeAndAfterAll { this: Suite =>

  implicit val system: ActorSystem = ActorSystem("testing", ConfigFactory.empty())
  val settings = ActorFlowMaterializerSettings(system)
  implicit val defaultMaterializer = ActorFlowMaterializer(settings)

  override protected def afterAll(): Unit = {
    super.afterAll()
    system.shutdown()
    system.awaitTermination()
  }
}
