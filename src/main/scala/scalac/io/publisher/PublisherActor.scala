package scalac.io.publisher

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import scalac.io.Models.Currency
import scalac.io.currencyapi.client.CurrencyApiClient

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration

object PublisherActor {
  def props(currencyApiClient: CurrencyApiClient, notifierService: RatesChangeNotifierService): Props =
    Props(new PublisherActor(currencyApiClient, notifierService))

  final case class Register(base: Currency, interval: FiniteDuration)
  final case class Cancel(base: Currency)
}

class PublisherActor(currencyApiClient: CurrencyApiClient,
                     notifierService: RatesChangeNotifierService) extends Actor with ActorLogging {

  import PublisherActor._

  private val registeredObservers: mutable.Map[Currency, ActorRef] = mutable.Map.empty

  override def receive: Receive = {
    case Register(base, interval) =>
      val ratesCheckerActor = context.system.actorOf(
        RatesCheckerActor.props(base, interval, currencyApiClient, notifierService)
      )

      registeredObservers + (base -> ratesCheckerActor)
      log.info(s"Registered observer for currency: ${base.symbol} with interval: $interval")

    case Cancel(base) =>
      registeredObservers.get(base).foreach(context.stop)
      registeredObservers - base
      log.info(s"Cancelled observation for currency: ${base.symbol}")

  }
}
