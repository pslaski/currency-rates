package scalac.io.http

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{RejectionHandler, Route}
import akka.stream.Materializer
import scalac.io.Models.Currency
import scalac.io.currencyapi.json.CurrencyApiJsonSupport._
import scalac.io.currencyapi.models.CurrencyApiResponses._
import scalac.io.currencyapi.services.CurrencyApiService
import scalac.io.utils.DateUtils
import scalac.io.utils.Unmarshallers._

import scala.util.{Failure, Success}

class CurrencyRoutes(currencyApiService: CurrencyApiService)
                    (implicit actorSystem: ActorSystem, mat: Materializer) extends SprayJsonSupport {

  private val logger = Logging(actorSystem, classOf[CurrencyRoutes])

  implicit def rejectionHandler = RejectionHandler.default

  def routes = Route.seal {
    rates
  }

  private def rates: Route = {
    path("rates") {
      parameters('base.as[Currency], 'target.as[Currency].?, 'timestamp.as[ZonedDateTime].?) { (base, target, timestamp) =>

        val zonedDateTimeUTC = timestamp.map(DateUtils.toUTC)

        val currencyApiResponseF = currencyApiService.getRates(base, zonedDateTimeUTC, target)

        onComplete(currencyApiResponseF) {
          case Success(currencyApiResponse) =>
            currencyApiResponse match {
              case successResp: SuccessResponse =>
                complete(successResp)

              case failureResp: FailureResponse =>
                complete(StatusCodes.BadRequest -> failureResp)
            }

          case Failure(ex) =>
            logger.error(ex, "Rates request failed")
            complete(StatusCodes.InternalServerError -> FailureResponse(ex.getMessage))
        }
      }
    }
  }

}
