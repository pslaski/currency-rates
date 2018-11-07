package scalac.io.currencyapi.fixer

import java.time.{LocalDate, ZonedDateTime}

import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model._
import org.scalatest.MustMatchers
import scalac.io.config.Models.FixerConfig
import scalac.io.currencyapi.models.CurrencyApiResponses.{CurrencyApiResponse, FailureResponse, CurrencyRatesResponse}
import scalac.io.http.HttpClient
import scalac.io.testUtils.{ActorSpec, ScalaFuturesConfigured}
import scalac.io.utils.CurrencyFixtures
import spray.json._

import scala.concurrent.Future

class FixerClientSpec extends ActorSpec("fixer-client-system") with MustMatchers with ScalaFuturesConfigured {

  private val accessKey = "ACCESS_TOKEN"
  private val fixerConfig = FixerConfig(accessKey, "/")

  private def createHttpClient(expectedReqUri: Uri, response: HttpResponse): HttpClient = (request: HttpRequest) => {
    assert(request.uri == expectedReqUri, s"Request uri not match an expected. Requested: [${request.uri}], expected: [$expectedReqUri]")
    Future.successful(response)
  }

  private def createFixerClient(httpClient: HttpClient): FixerClient = new FixerClient(fixerConfig, httpClient)

  "FixerClient" should {
    "return all latest rates" in new LatestTestContext {
      override val jsonStr =
        s"""
           |{
           |    "success": true,
           |    "timestamp": 1519296206,
           |    "base": "USD",
           |    "date": "2018-11-05",
           |    "rates": {
           |        "GBP": 0.72007,
           |        "JPY": 107.346001,
           |        "EUR": 0.813399
           |    }
           |}
         """.stripMargin

      override val expectedUri = Uri("/latest?access_key=ACCESS_TOKEN&base=USD")

      override val resp = fixerClient.getLatestRates(usdCurrency).futureValue

      override val expectedResp = CurrencyRatesResponse(
        date = LocalDate.parse("2018-11-05"),
        timestamp = 1519296206,
        base = usdCurrency,
        rates = rates)

      assertResponses()
    }

    "return latest rates for target" in new LatestTestContext {
      override val jsonStr =
      s"""
         |{
         |    "success": true,
         |    "timestamp": 1519296206,
         |    "base": "USD",
         |    "date": "2018-11-05",
         |    "rates": {
         |        "EUR": 0.813399
         |    }
         |}
         """.stripMargin

      override val expectedUri = Uri("/latest?access_key=ACCESS_TOKEN&base=USD&symbols=EUR")

      override val resp = fixerClient.getLatestRates(usdCurrency, Some(eurCurrency)).futureValue

      override val expectedResp = CurrencyRatesResponse(
        date = LocalDate.parse("2018-11-05"),
        timestamp = 1519296206,
        base = usdCurrency,
        rates = shortRates)

      assertResponses()
    }

    "return historical rates" in new HistoricalTestContext {
      override val jsonStr =
      s"""
         |{
         |    "success": true,
         |    "historical": true,
         |    "date": "2013-12-24",
         |    "timestamp": 1387929599,
         |    "base": "USD",
         |    "rates": {
         |        "GBP": 0.72007,
         |        "JPY": 107.346001,
         |        "EUR": 0.813399
         |    }
         |}
         """.stripMargin

      override val expectedUri = Uri("/2013-12-24?access_key=ACCESS_TOKEN&base=USD")

      override val resp = fixerClient.getHistoricalRates(usdCurrency, timestamp).futureValue

      override val expectedResp = CurrencyRatesResponse(
        date = LocalDate.parse("2013-12-24"),
        timestamp = 1387929599,
        base = usdCurrency,
        rates = rates)

      assertResponses()
    }

    "return historical rates for target" in new HistoricalTestContext {
      override val jsonStr =
        s"""
           |{
           |    "success": true,
           |    "historical": true,
           |    "date": "2013-12-24",
           |    "timestamp": 1387929599,
           |    "base": "USD",
           |    "rates": {
           |        "EUR": 0.813399
           |    }
           |}
         """.stripMargin

      override val expectedUri = Uri("/2013-12-24?access_key=ACCESS_TOKEN&base=USD&symbols=EUR")

      override val resp = fixerClient.getHistoricalRates(usdCurrency, timestamp, Some(eurCurrency)).futureValue

      override val expectedResp = CurrencyRatesResponse(
        date = LocalDate.parse("2013-12-24"),
        timestamp = 1387929599,
        base = usdCurrency,
        rates = shortRates)

      assertResponses()
    }

    "return failure response" when {
      "latest rates request failed" in new LatestTestContext with FailureTestContext {

        override val jsonStr = failureJsonStr

        override val responseStatus = failureResponseStatus

        override val expectedUri = Uri("/latest?access_key=ACCESS_TOKEN&base=USD")

        override val resp = fixerClient.getLatestRates(usdCurrency).futureValue

        override val expectedResp = FailureResponse(msg)

        assertResponses()
      }

      "historical rates request failed" in new HistoricalTestContext with FailureTestContext {

        override val jsonStr = failureJsonStr

        override val responseStatus = failureResponseStatus

        override val expectedUri = Uri("/2013-12-24?access_key=ACCESS_TOKEN&base=USD")

        override val resp = fixerClient.getHistoricalRates(usdCurrency, timestamp).futureValue

        override val expectedResp = FailureResponse(msg)

        assertResponses()
      }
    }
  }

  trait LatestTestContext extends CurrencyFixtures {
    def jsonStr: String
    lazy val json = jsonStr.parseJson

    def responseStatus: StatusCode = StatusCodes.OK
    def expectedUri: Uri
    lazy val apiResponse = HttpResponse()
      .withStatus(responseStatus)
      .withEntity(HttpEntity(`application/json`, json.compactPrint))

    lazy val httpClient = createHttpClient(expectedUri, apiResponse)
    lazy val fixerClient = createFixerClient(httpClient)

    def resp: CurrencyApiResponse
    def expectedResp: CurrencyApiResponse

    def assertResponses() = {
      resp mustBe expectedResp
    }
  }

  trait HistoricalTestContext extends LatestTestContext {
    val timestamp = ZonedDateTime.parse("2013-12-24T14:34:46Z")
  }

  trait FailureTestContext {
    val msg = "Your monthly API request volume has been reached. Please upgrade your plan."
    val failureJsonStr =
      s"""
         |{
         |  "success": false,
         |  "error": {
         |    "code": 404,
         |    "info": "$msg"
         |  }
         |}
         """.stripMargin

    val failureResponseStatus = StatusCodes.NotFound
  }

}
