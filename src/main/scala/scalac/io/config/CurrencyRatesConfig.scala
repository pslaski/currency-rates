package scalac.io.config

import com.typesafe.config.Config
import scalac.io.config.Models.FixerConfig

object CurrencyRatesConfig {

  def getFixerConfig(config: Config): FixerConfig = {
    FixerConfig(
      accessKey = config.getString("currency-rates.fixer.access-key"),
      baseUri = config.getString("currency-rates.fixer.base-uri")
    )
  }

}
