package scalac.io.currencyapi.fixer

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.model.Uri.{Path, Query}
import akka.http.scaladsl.model.{HttpRequest, StatusCodes, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import scalac.io.Models._
import scalac.io.currencyapi.client.CurrencyApiClient
import scalac.io.currencyapi.models.CurrencyApiResponses._
import scalac.io.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

class FixerClient(baseUri: Uri, accessKey: String, httpClient: HttpClient)
                 (implicit actorSystem: ActorSystem, mat: Materializer, ec: ExecutionContext)
  extends CurrencyApiClient with JsonSupport {

  val logger = Logging(actorSystem, classOf[FixerClient])

  override def getLatestRates(base: Currency, target: Option[Currency]): Future[CurrencyApiResponse] = {
    val uri = baseUri
      .withPath(Path("/latest"))
      .withQuery(createQuery(base, target))

    sendRequest(HttpRequest().withUri(uri))
  }

  override def getHistoricalRates(base: Currency, datetime: ZonedDateTime, target: Option[Currency]): Future[CurrencyApiResponse] = {
    val uri = baseUri
      .withPath(Path./(datetime.toLocalDate.toString))
      .withQuery(createQuery(base, target))

    sendRequest(HttpRequest().withUri(uri))
  }

  private def createQuery(base: Currency, target: Option[Currency]): Query = {
    val defaultParams = Map("access_key" -> accessKey, "base" -> base.symbol)
    val symbolsParam = target.map { currency =>
      Map("symbols" -> currency.symbol)
    }.getOrElse(Map.empty)

    Query(defaultParams ++ symbolsParam)
  }

  private def sendRequest(request: HttpRequest): Future[CurrencyApiResponse] = {

    logger.debug(s"Sending request to: ${request.uri}")

    httpClient.sendRequest(request).flatMap { response =>
      response.status match {
        case status if status == StatusCodes.OK =>
          Unmarshal(response.entity).to[SuccessResponse]
        case _ =>
          logger.debug(s"Request is not successful. Response status: ${response.status}")
          Unmarshal(response.entity).to[FailureResponse]
      }
    }
  }
}
