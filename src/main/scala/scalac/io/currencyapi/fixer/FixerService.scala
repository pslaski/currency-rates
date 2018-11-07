package scalac.io.currencyapi.fixer

import java.time.ZonedDateTime

import scalac.io.Models._
import scalac.io.currencyapi.client.CurrencyApiClient
import scalac.io.currencyapi.models.CurrencyApiResponses.CurrencyApiResponse
import scalac.io.currencyapi.services.CurrencyApiService

import scala.concurrent.Future

class FixerService(currencyApiClient: CurrencyApiClient) extends CurrencyApiService{
  override def getRates(base: Currency, datetime: Option[ZonedDateTime], target: Option[Currency]): Future[CurrencyApiResponse] = {
    datetime match {
      case Some(zonedDateTime) =>
        currencyApiClient.getHistoricalRates(base, zonedDateTime, target)
      case None =>
        currencyApiClient.getLatestRates(base, target)
    }
  }
}
