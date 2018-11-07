package scalac.io.testUtils

import org.scalatest.concurrent.ScalaFutures

trait ScalaFuturesConfigured extends ScalaFutures {

  implicit override val patienceConfig = PatienceConfig()

}
