package scalac.io.publisher

import scalac.io.currencyapi.models.CurrencyApiResponses.CurrencyRatesResponse

import scala.concurrent.Future

trait RatesChangeNotifierService {

  def notify(latestRates: CurrencyRatesResponse): Future[CurrencyRatesResponse]

}
