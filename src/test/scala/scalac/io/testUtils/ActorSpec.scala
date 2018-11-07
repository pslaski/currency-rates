package scalac.io.testUtils

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.scalatest.WordSpecLike

import scala.concurrent.ExecutionContext

abstract class ActorSpec(system: String) extends WordSpecLike {

  implicit val actorSystem: ActorSystem = ActorSystem(system)
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = actorSystem.dispatcher

}
