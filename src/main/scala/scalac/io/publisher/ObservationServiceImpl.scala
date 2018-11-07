package scalac.io.publisher
import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import scalac.io.Models
import scalac.io.publisher.RatesObserversManager.{Cancel, Register}

import scala.concurrent.Future
import scala.concurrent.duration.{FiniteDuration, _}

class ObservationServiceImpl(ratesObserversManager: ActorRef)
                            (implicit actorSystem: ActorSystem) extends ObservationService {

  private implicit val timeout: Timeout = Timeout(5 seconds)

  override def register(base: Models.Currency, interval: FiniteDuration): Future[Boolean] = {
    (ratesObserversManager ? Register(base, interval)).mapTo[Boolean]
  }

  override def cancel(base: Models.Currency): Future[Boolean] = {
    (ratesObserversManager ? Cancel(base)).mapTo[Boolean]
  }
}
