package scalac.io.currencyapi.models

import java.time.LocalDate

import scalac.io.Models.{Currency, CurrencyRates}

object CurrencyApiResponses {

  sealed trait CurrencyApiResponse

  case class SuccessResponse(date: LocalDate, base: Currency, rates: CurrencyRates) extends CurrencyApiResponse

  case class FailureResponse(message: String) extends CurrencyApiResponse

}
