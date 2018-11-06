package scalac.io

object Models {

  case class Currency(symbol: String) extends AnyVal

  type CurrencyRates = Map[Currency, BigDecimal]

}
