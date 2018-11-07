package scalac.io.publisher

import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, MustMatchers, WordSpecLike}
import scalac.io.Models.Currency
import scalac.io.currencyapi.client.CurrencyApiClient
import scalac.io.currencyapi.models.CurrencyApiResponses._
import scalac.io.publisher.RatesObserversManager.{Cancel, Register}
import scalac.io.utils.CurrencyFixtures

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class RatesObserversManagerSpec extends TestKit(ActorSystem("RatesObserversManagerSpec")) with ImplicitSender
  with WordSpecLike with MustMatchers with BeforeAndAfterAll with CurrencyFixtures {

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  private val interval = FiniteDuration(1, TimeUnit.SECONDS)
  private val failedResp = Future.failed(new RuntimeException("error"))

  private val notifyService = new RatesChangeNotifierService {
    override def notify(latestRates: CurrencyRatesResponse): Future[CurrencyRatesResponse] = failedResp
  }

  private val currencyApiClient = new CurrencyApiClient {
    override def getLatestRates(base: Currency, target: Option[Currency]): Future[CurrencyApiResponse] = failedResp

    override def getHistoricalRates(base: Currency, datetime: ZonedDateTime, target: Option[Currency]): Future[CurrencyApiResponse] = ???
  }

  private def createTestActor = {
    system.actorOf(RatesObserversManager.props(currencyApiClient, notifyService))
  }

  "RatesObserversManager" should {
    "return true" when {
      "register successfully new observer" in {
        val actor = createTestActor

        actor ! Register(usdCurrency, interval)

        expectMsg(true)
        expectNoMessage()

        system.stop(actor)
      }

      "cancel successfully observation" in {

        val actor = createTestActor

        actor ! Register(usdCurrency, interval)

        expectMsg(true)

        actor ! Cancel(usdCurrency)

        expectMsg(true)
        expectNoMessage()

        system.stop(actor)

      }
    }

    "return false" when {
      "try to register new observer to already observed currency" in {
        val actor = createTestActor

        actor ! Register(usdCurrency, interval)

        expectMsg(true)

        actor ! Register(usdCurrency, interval)

        expectMsg(false)
        expectNoMessage()

        system.stop(actor)
      }

      "try to cancel not existing observation" in {
        val actor = createTestActor

        actor ! Cancel(usdCurrency)

        expectMsg(false)
        expectNoMessage()

        system.stop(actor)
      }
    }
  }

}
