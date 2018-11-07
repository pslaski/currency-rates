package scalac.io.config

import akka.http.scaladsl.model.Uri
import com.typesafe.config.Config
import scalac.io.config.Models.{FixerConfig, PublishingConfig}

object CurrencyRatesConfig {

  def getFixerConfig(config: Config): FixerConfig = {
    FixerConfig(
      accessKey = config.getString("currency-rates.fixer.access-key"),
      baseUri = config.getString("currency-rates.fixer.base-uri")
    )
  }

  def getPublishingConfig(config: Config): PublishingConfig = {
    PublishingConfig(webhookUri = Uri(config.getString("currency-rates.publishing.webhook-uri")))
  }

}
