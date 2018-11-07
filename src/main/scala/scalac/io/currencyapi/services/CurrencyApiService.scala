package scalac.io.currencyapi.services

import java.time.ZonedDateTime

import scalac.io.Models.Currency
import scalac.io.currencyapi.models.CurrencyApiResponses.CurrencyApiResponse

import scala.concurrent.Future

trait CurrencyApiService {

  def getRates(base: Currency, datetime: Option[ZonedDateTime], target: Option[Currency]): Future[CurrencyApiResponse]

}
