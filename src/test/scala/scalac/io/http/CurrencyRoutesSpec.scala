package scalac.io.http

import java.time.ZonedDateTime

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{MustMatchers, WordSpec}
import scalac.io.Models._
import scalac.io.currencyapi.models.CurrencyApiResponses.{CurrencyApiResponse, FailureResponse, SuccessResponse}
import scalac.io.currencyapi.services.CurrencyApiService
import scalac.io.testUtils.ScalaFuturesConfigured
import scalac.io.utils.CurrencyFixtures

import scala.concurrent.Future

class CurrencyRoutesSpec extends WordSpec with ScalatestRouteTest with ScalaFuturesConfigured with MustMatchers with SprayJsonSupport {

  import scalac.io.currencyapi.json.CurrencyApiJsonSupport._

  private def createCurrencyApiService(response: CurrencyApiResponse, expectedTarget: Option[Currency] = None, expectedDateTime: Option[ZonedDateTime] = None) = new CurrencyApiService {
    override def getRates(base: Currency, datetime: Option[ZonedDateTime], target: Option[Currency]): Future[CurrencyApiResponse] = {
      assert(expectedTarget == target, s"Target parameter is different than expected. Expected: $expectedTarget, got: $target")
      assert(expectedDateTime == datetime, s"Timestamp parameter is different than expected. Expected: $expectedDateTime, got: $datetime")
      Future.successful(response)
    }
  }

  private def createRoutes(currencyApiService: CurrencyApiService) = new CurrencyRoutes(currencyApiService)

  "CurrencyRoutes" should {
    "return rates route success results" when {
      "asking for latest rates for given currency" in new TestContext {
        override val serviceResp = SuccessResponse(localDate, usdCurrency, rates)
        Get("/rates?base=USD") ~> route ~> check {
          status mustBe StatusCodes.OK
          responseAs[SuccessResponse] mustBe serviceResp
        }
      }

      "asking for latest rates for given currency and target" in new TestContext {
        override val serviceResp = SuccessResponse(localDate, usdCurrency, shortRates)
        override val expectedTarget = Some(eurCurrency)
        Get("/rates?base=USD&target=EUR") ~> route ~> check {
          status mustBe StatusCodes.OK
          responseAs[SuccessResponse] mustBe serviceResp
        }
      }

      "asking for historical rates for given currency" in new TestContext {
        override val expectedTimestamp = Some(historicalDate)
        override val serviceResp = SuccessResponse(historicalLocalDate, usdCurrency, rates)
        Get("/rates?base=USD&timestamp=2016-04-29T14:34:46Z") ~> route ~> check {
          status mustBe StatusCodes.OK
          responseAs[SuccessResponse] mustBe serviceResp
        }
      }

      "asking for historical rates for given currency and target" in new TestContext {
        override val expectedTimestamp = Some(historicalDate)
        override val expectedTarget = Some(eurCurrency)
        override val serviceResp = SuccessResponse(historicalLocalDate, usdCurrency, shortRates)
        Get("/rates?base=USD&target=EUR&timestamp=2016-04-29T14:34:46Z") ~> route ~> check {
          status mustBe StatusCodes.OK
          responseAs[SuccessResponse] mustBe serviceResp
        }
      }
    }

    "return rates route failure results" when {
      "external api return failure" in new TestContext {
        val failureMsg = "Some failure message"
        override val serviceResp = FailureResponse(failureMsg)

        Get("/rates?base=USD") ~> route ~> check {
          status mustBe StatusCodes.BadRequest
          responseAs[String] mustBe s"""{"error":"$failureMsg"}"""
        }
      }

      "request to external api failed" in {
        val failureService = new CurrencyApiService {
          override def getRates(base: Currency, datetime: Option[ZonedDateTime], target: Option[Currency]): Future[CurrencyApiResponse] = {
            Future.failed(new RuntimeException("Request failed"))
          }
        }

        val route = createRoutes(failureService).routes

        Get("/rates?base=USD") ~> route ~> check {
          status mustBe StatusCodes.InternalServerError
          responseAs[String] mustBe s"""{"error":"Request failed"}"""
        }
      }

    }
  }

  trait TestContext extends CurrencyFixtures {
    val timestamp = "2016-05-01T14:34:46Z"
    val localDate = ZonedDateTime.parse(timestamp).toLocalDate
    val historicalDate = ZonedDateTime.parse("2016-04-29T14:34:46Z")
    val historicalLocalDate = historicalDate.toLocalDate
    def serviceResp: CurrencyApiResponse
    def expectedTarget: Option[Currency] = None
    def expectedTimestamp: Option[ZonedDateTime] = None
    def route = createRoutes(createCurrencyApiService(serviceResp, expectedTarget, expectedTimestamp)).routes
  }

}
