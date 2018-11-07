package scalac.io.publisher

import scalac.io.Models.Currency

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

trait ObservationService {

  def register(base: Currency, interval: FiniteDuration): Future[Boolean]

  def cancel(base: Currency): Future[Boolean]

}
