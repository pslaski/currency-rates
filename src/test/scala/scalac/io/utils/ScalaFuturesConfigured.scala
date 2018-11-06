package scalac.io.utils

import org.scalatest.concurrent.ScalaFutures

trait ScalaFuturesConfigured extends ScalaFutures {

  implicit override val patienceConfig = PatienceConfig()

}
