package scalac.io.publisher

import java.time.{Instant, ZonedDateTime}
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, MustMatchers, WordSpecLike}
import scalac.io.Models.Currency
import scalac.io.currencyapi.client.CurrencyApiClient
import scalac.io.currencyapi.models.CurrencyApiResponses._
import scalac.io.utils.CurrencyFixtures

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class RatesCheckerActorSpec extends TestKit(ActorSystem("RatesCheckerActorSpec")) with ImplicitSender
  with WordSpecLike with MustMatchers with BeforeAndAfterAll with CurrencyFixtures {

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  private val testProbe = TestProbe()
  private val interval = FiniteDuration(1, TimeUnit.SECONDS)
  private val localDate = ZonedDateTime.parse("2016-04-30T23:34:46Z").toLocalDate
  private val timestamp = Instant.now().toEpochMilli
  private val ratesMsg = CurrencyRatesResponse(localDate, timestamp, usdCurrency, rates)

  private val notifyService = new RatesChangeNotifierService {
    override def notify(latestRates: CurrencyRatesResponse): Future[CurrencyRatesResponse] = {
      testProbe.ref ! latestRates
      Future.successful(latestRates)
    }
  }

  private def createTestActor(currencyApiClient: CurrencyApiClient) = {
    system.actorOf(RatesCheckerActor.props(usdCurrency, interval, currencyApiClient, notifyService))
  }

  trait CurrencyApiClientStub extends CurrencyApiClient {
    override def getHistoricalRates(base: Currency, datetime: ZonedDateTime, target: Option[Currency]): Future[CurrencyApiResponse] = ???
  }

  "RatesCheckerActor" should {
    "ask for latest rates and notify only when it changes" in {

      val currencyApiClient = new CurrencyApiClientStub {
        override def getLatestRates(base: Currency, target: Option[Currency]): Future[CurrencyApiResponse] = {
          Future.successful(ratesMsg)
        }
      }

      val actor = createTestActor(currencyApiClient)

      testProbe.expectMsg(FiniteDuration(3, TimeUnit.SECONDS), ratesMsg)
      testProbe.expectNoMessage()

      system.stop(actor)

    }

    "ask for latest rates and notify when it changes" in new CurrencyFixtures {

      val newRatesMsg = ratesMsg.copy(rates = shortRates)

      val currencyApiClient = new CurrencyApiClientStub {
        val responses = mutable.ArrayBuffer(newRatesMsg, ratesMsg)
        override def getLatestRates(base: Currency, target: Option[Currency]): Future[CurrencyApiResponse] = {
          val resp = responses match {
            case ArrayBuffer(elem) => elem
            case ArrayBuffer(_, elem2) => {
              responses -= elem2
              elem2
            }
          }

          Future.successful(resp)
        }
      }

      val actor = createTestActor(currencyApiClient)

      testProbe.expectMsg(FiniteDuration(2, TimeUnit.SECONDS), ratesMsg)
      testProbe.expectMsg(FiniteDuration(2, TimeUnit.SECONDS), newRatesMsg)
      testProbe.expectNoMessage()

      system.stop(actor)

    }

    "ask for latest rates and don't notify when service return failure response" in {

      val currencyApiClient = new CurrencyApiClientStub {
        override def getLatestRates(base: Currency, target: Option[Currency]): Future[CurrencyApiResponse] = {
          Future.successful(FailureResponse("Something went wrong"))
        }
      }

      val actor = createTestActor(currencyApiClient)

      testProbe.expectNoMessage()

      system.stop(actor)

    }

    "ask for latest rates and don't notify when request failed" in {

      val currencyApiClient = new CurrencyApiClientStub {
        override def getLatestRates(base: Currency, target: Option[Currency]): Future[CurrencyApiResponse] = {
          Future.failed(new RuntimeException("Something went wrong"))
        }
      }

      val actor = createTestActor(currencyApiClient)

      testProbe.expectNoMessage()

      system.stop(actor)

    }
  }

}
