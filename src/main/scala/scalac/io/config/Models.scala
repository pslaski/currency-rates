package scalac.io.config

import akka.http.scaladsl.model.Uri

object Models {

  case class FixerConfig(accessKey: String, baseUri: String)

  case class PublishingConfig(webhookUri: Uri)

}
