package scalac.io.http

import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

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
import scalac.io.publisher.ObservationService
import scalac.io.utils.DateUtils
import scalac.io.utils.Unmarshallers._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

class CurrencyRoutes(currencyApiService: CurrencyApiService,
                     observationService: ObservationService)
                    (implicit actorSystem: ActorSystem, mat: Materializer, ec: ExecutionContext) extends SprayJsonSupport {

  private val logger = Logging(actorSystem, classOf[CurrencyRoutes])

  implicit def rejectionHandler = RejectionHandler.default

  def routes = Route.seal {
    rates ~ publishing
  }

  private def rates: Route = {
    path("rates") {
      parameters('base.as[Currency], 'target.as[Currency].?, 'timestamp.as[ZonedDateTime].?) { (base, target, timestamp) =>

        val zonedDateTimeUTC = timestamp.map(DateUtils.toUTC)

        val currencyApiResponseF = currencyApiService.getRates(base, zonedDateTimeUTC, target)

        onComplete(currencyApiResponseF) {
          case Success(currencyApiResponse) =>
            currencyApiResponse match {
              case successResp: CurrencyRatesResponse =>
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

  private def publishing: Route = {
    pathPrefix("publishing") {
      path(Segment / LongNumber) { (base, interval) =>
        val currency = Currency(base)
        val intervalInSeconds = FiniteDuration(interval, TimeUnit.SECONDS)

        registerObserver(currency, intervalInSeconds)
      } ~
        path(Segment) { base =>
          val currency = Currency(base)

          cancelObserver(currency)
        }
    }
  }

  private def registerObserver(base: Currency, interval: FiniteDuration): Route = {
    post {
      val registerF = observationService.register(base, interval)

      onComplete(registerF) {
        case Success(isSuccess) =>
          if(isSuccess) {
            complete(s"Observer for currency: ${base.symbol} registered. Check interval is $interval")
          } else {
            complete(StatusCodes.BadRequest, s"Observer for currency: ${base.symbol} is already registered")
          }
        case Failure(ex) =>
          val msg = s"Registering observer for currency: ${base.symbol} failed"
          logger.error(ex, msg)
          complete(StatusCodes.InternalServerError -> msg)
      }
    }
  }

  private def cancelObserver(base: Currency): Route = {
    delete {
      val cancelF = observationService.cancel(base)

      onComplete(cancelF) {
        case Success(isSuccess) =>
          if(isSuccess) {
            complete(s"Observer for currency: ${base.symbol} was stopped")
          } else {
            complete(StatusCodes.BadRequest, s"Observer for currency: ${base.symbol} is not registered")
          }
        case Failure(ex) =>
          val msg = s"Canceling observer for currency: ${base.symbol} failed"
          logger.error(ex, msg)
          complete(StatusCodes.InternalServerError -> msg)
      }
    }
  }

}
