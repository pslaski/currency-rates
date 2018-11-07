package scalac.io.publisher
import akka.actor.ActorSystem
import akka.http.scaladsl.model.ContentTypes.`application/json`
import akka.http.scaladsl.model.{HttpEntity, HttpMethods, HttpRequest, Uri}
import scalac.io.currencyapi.json.CurrencyApiJsonSupport._
import scalac.io.currencyapi.models.CurrencyApiResponses
import scalac.io.http.HttpClient
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

class RatesChangeNotifierServiceImpl(httpClient: HttpClient,
                                     uri: Uri)
                                    (implicit actorSystem: ActorSystem, ec: ExecutionContext) extends RatesChangeNotifierService {
  override def notify(latestRates: CurrencyApiResponses.CurrencyRatesResponse): Future[CurrencyApiResponses.CurrencyRatesResponse] = {
    val entity = HttpEntity(`application/json`, latestRates.toJson.compactPrint)
    val req = HttpRequest()
      .withUri(uri)
      .withMethod(HttpMethods.POST)
      .withEntity(entity)

    httpClient.sendRequest(req).map(_ => latestRates)
  }
}
