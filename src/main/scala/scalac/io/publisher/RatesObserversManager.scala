package scalac.io.publisher

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import scalac.io.Models.Currency
import scalac.io.currencyapi.client.CurrencyApiClient

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration

object RatesObserversManager {
  def props(currencyApiClient: CurrencyApiClient, notifierService: RatesChangeNotifierService): Props =
    Props(new RatesObserversManager(currencyApiClient, notifierService))

  final case class Register(base: Currency, interval: FiniteDuration)
  final case class Cancel(base: Currency)
}

class RatesObserversManager(currencyApiClient: CurrencyApiClient,
                            notifierService: RatesChangeNotifierService) extends Actor with ActorLogging {

  import RatesObserversManager._

  private val registeredObservers: mutable.Map[Currency, ActorRef] = mutable.Map.empty

  override def receive: Receive = {
    case Register(base, interval) =>
      if(registeredObservers.contains(base)) {
        log.info(s"Observer for currency: ${base.symbol} already exists")
        sender() ! false
      } else {
        val ratesCheckerActor = context.system.actorOf(
          RatesCheckerActor.props(base, interval, currencyApiClient, notifierService)
        )

        registeredObservers += (base -> ratesCheckerActor)
        log.info(s"Registered observer for currency: ${base.symbol} with interval: $interval")
        sender() ! true
      }

    case Cancel(base) =>
      if(registeredObservers.contains(base)) {
        registeredObservers.get(base).foreach(context.stop)
        registeredObservers -= base
        log.info(s"Cancelled observation for currency: ${base.symbol}")
        sender() ! true
      } else {
        log.info(s"Trying to cancel observation for currency: ${base.symbol}")
        sender() ! false
      }

  }
}
