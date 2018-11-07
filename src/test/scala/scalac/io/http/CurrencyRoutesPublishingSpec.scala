package scalac.io.http

import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{MustMatchers, WordSpec}
import scalac.io.Models._
import scalac.io.currencyapi.models.CurrencyApiResponses.{CurrencyApiResponse, FailureResponse}
import scalac.io.currencyapi.services.CurrencyApiService
import scalac.io.publisher.ObservationService
import scalac.io.testUtils.ScalaFuturesConfigured
import scalac.io.utils.CurrencyFixtures

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class CurrencyRoutesPublishingSpec extends WordSpec with ScalatestRouteTest with ScalaFuturesConfigured with MustMatchers with SprayJsonSupport {

  private val currencyApiService = new CurrencyApiService {
    override def getRates(base: Currency, datetime: Option[ZonedDateTime], target: Option[Currency]): Future[CurrencyApiResponse] =
      Future.successful(FailureResponse("some msg"))
  }

  private def createObservationService(response: Boolean, expectedBase: Currency, expectedInterval: FiniteDuration) = new ObservationService {
    override def register(base: Currency, interval: FiniteDuration): Future[Boolean] = {
      assert(expectedBase == base, s"Base parameter is different than expected. Expected: $expectedBase, got: $base")
      assert(expectedInterval == interval, s"Interval parameter is different than expected. Expected: $expectedInterval, got: $interval")
      Future.successful(response)
    }

    override def cancel(base: Currency): Future[Boolean] = {
      assert(expectedBase == base, s"Base parameter is different than expected. Expected: $expectedBase, got: $base")
      Future.successful(response)
    }
  }

  private val failureObservationService = new ObservationService {
    override def register(base: Currency, interval: FiniteDuration): Future[Boolean] =
      Future.failed(new RuntimeException("Register failed"))

    override def cancel(base: Currency): Future[Boolean] =
      Future.failed(new RuntimeException("Cancel failed"))
  }

  private def createRoutes(observationService: ObservationService) = new CurrencyRoutes(currencyApiService, observationService)

  "CurrencyRoutes.publishing.register" should {
    "register new observer for given currency and interval" in new TestContext {
      override val expectedServiceResponse = true
      Post("/publishing/USD/10") ~> route ~> check {
        status mustBe StatusCodes.OK
        responseAs[String] mustBe "Observer for currency: USD registered. Check interval is 10 seconds"
      }
    }

    "return BadRequest with msg when observer for given currency already exists" in new TestContext {
      override val expectedServiceResponse = false
      Post("/publishing/USD/10") ~> route ~> check {
        status mustBe StatusCodes.BadRequest
        responseAs[String] mustBe "Observer for currency: USD is already registered"
      }
    }

    "return InternalServerError with msg when something went wrong" in {
      val route = createRoutes(failureObservationService).routes
      Post("/publishing/USD/10") ~> route ~> check {
        status mustBe StatusCodes.InternalServerError
        responseAs[String] mustBe "Registering observer for currency: USD failed"
      }
    }

  }

  "CurrencyRoutes.publishing.cancel" should {
    "cancel observation for given currency" in new TestContext {
      override val expectedServiceResponse = true
      Delete("/publishing/USD") ~> route ~> check {
        status mustBe StatusCodes.OK
        responseAs[String] mustBe "Observer for currency: USD was stopped"
      }
    }

    "return BadRequest with msg when observer for given currency is not registered" in new TestContext {
      override val expectedServiceResponse = false
      Delete("/publishing/USD") ~> route ~> check {
        status mustBe StatusCodes.BadRequest
        responseAs[String] mustBe "Observer for currency: USD is not registered"
      }
    }

    "return InternalServerError with msg when something went wrong" in {
      val route = createRoutes(failureObservationService).routes
      Delete("/publishing/USD") ~> route ~> check {
        status mustBe StatusCodes.InternalServerError
        responseAs[String] mustBe "Canceling observer for currency: USD failed"
      }
    }

  }

  trait TestContext extends CurrencyFixtures {
    val expectedBase: Currency = usdCurrency
    val expectedInterval: FiniteDuration = FiniteDuration(10, TimeUnit.SECONDS)
    def expectedServiceResponse: Boolean
    def route = createRoutes(createObservationService(response = expectedServiceResponse, expectedBase, expectedInterval)).routes
  }

}
