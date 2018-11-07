package scalac.io.utils

import scalac.io.Models.{Currency, CurrencyRates}

trait CurrencyFixtures {

  val usdCurrency = Currency("USD")
  val gbpCurrency = Currency("GBP")
  val jpyCurrency = Currency("JPY")
  val eurCurrency = Currency("EUR")

  val rates: CurrencyRates = Map(
    gbpCurrency -> BigDecimal(0.72007),
    jpyCurrency -> BigDecimal(107.346001),
    eurCurrency -> BigDecimal(0.813399)
  )

  val shortRates = rates -- Seq(gbpCurrency, jpyCurrency)

}
