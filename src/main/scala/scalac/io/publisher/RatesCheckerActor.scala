package scalac.io.publisher

import akka.actor.Status.Failure
import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.pipe
import scalac.io.Models.Currency
import scalac.io.currencyapi.client.CurrencyApiClient
import scalac.io.currencyapi.models.CurrencyApiResponses.{CurrencyRatesResponse, FailureResponse}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

object RatesCheckerActor {

  def props(base: Currency, interval: FiniteDuration, currencyApiClient: CurrencyApiClient, notifierService: RatesChangeNotifierService): Props =
    Props(new RatesCheckerActor(base, interval, currencyApiClient, notifierService))

  case object Check
}

class RatesCheckerActor(base: Currency,
                        interval: FiniteDuration,
                        currencyApiClient: CurrencyApiClient,
                        notifierService: RatesChangeNotifierService) extends Actor with ActorLogging {

  import RatesCheckerActor._

  implicit private val ec: ExecutionContext = context.dispatcher

  var latestRates: Option[CurrencyRatesResponse] = None

  override def preStart() = scheduleCheck

  // override postRestart so we don't call preStart and schedule a new message
  override def postRestart(reason: Throwable) = {}

  override def receive: Receive = {
    case Check =>
      scheduleCheck
      currencyApiClient.getLatestRates(base) pipeTo self

    case rates: CurrencyRatesResponse =>
      if(isRatesChanged(rates)) {
        latestRates = Some(rates)
        notifierService.notify(rates)
      }

    case failure: FailureResponse =>
      log.error(s"Currency API returned error: ${failure.message}")

    case Failure(ex) =>
      log.error(ex, "Request to Currency API failed")
  }

  private def scheduleCheck = {
    context.system.scheduler.scheduleOnce(interval, self, Check)
  }

  private def isRatesChanged(ratesFromApi: CurrencyRatesResponse): Boolean = {
    latestRates.exists { savedRates =>
      savedRates.rates != ratesFromApi.rates
    }
  }
}
