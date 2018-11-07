package scalac.io.publisher
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import scalac.io.Models
import scalac.io.currencyapi.client.CurrencyApiClient
import scalac.io.publisher.RatesObserversManager.{Cancel, Register}

import scala.concurrent.Future
import scala.concurrent.duration.{FiniteDuration, _}

class ObservationServiceImpl(currencyApiClient: CurrencyApiClient,
                             notifierService: RatesChangeNotifierService)
                            (implicit actorSystem: ActorSystem) extends ObservationService {

  private val ratesObserversManager = actorSystem.actorOf(RatesObserversManager.props(currencyApiClient, notifierService))
  private implicit val timeout: Timeout = Timeout(5 seconds)

  override def register(base: Models.Currency, interval: FiniteDuration): Future[Boolean] = {
    (ratesObserversManager ? Register(base, interval)).mapTo[Boolean]
  }

  override def cancel(base: Models.Currency): Future[Boolean] = {
    (ratesObserversManager ? Cancel(base)).mapTo[Boolean]
  }
}
