package scalac.io.currencyapi.client

import java.time.ZonedDateTime

import scalac.io.Models.Currency
import scalac.io.currencyapi.models.CurrencyApiResponses.CurrencyApiResponse

import scala.concurrent.Future

trait CurrencyApiClient {

  def getLatestRates(base: Currency, target: Option[Currency] = None): Future[CurrencyApiResponse]

  def getHistoricalRates(base: Currency, datetime: ZonedDateTime, target: Option[Currency] = None): Future[CurrencyApiResponse]

}
