package scalac.io.currencyapi.json

import scalac.io.currencyapi.models.CurrencyApiResponses.{FailureResponse, CurrencyRatesResponse}
import spray.json._

trait CurrencyApiJsonSupport {

  import scalac.io.json.JsonSupport._
  import spray.json.DefaultJsonProtocol._

  implicit val successResponseFormat = jsonFormat4(CurrencyRatesResponse)

  implicit val failureResponseWriter = new RootJsonWriter[FailureResponse] {
    override def write(obj: FailureResponse): JsValue = {
      JsObject("error" -> JsString(obj.message))
    }
  }

}

object CurrencyApiJsonSupport extends CurrencyApiJsonSupport