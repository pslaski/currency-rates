package scalac.io.currencyapi.fixer

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import scalac.io.currencyapi.models.CurrencyApiResponses.FailureResponse
import spray.json._

trait FixerJsonSupport extends SprayJsonSupport {
  import spray.json.DefaultJsonProtocol._

  implicit val failureResponseReader = new RootJsonReader[FailureResponse] {
    override def read(json: JsValue): FailureResponse = {
      val errorMsg = json
        .asJsObject
        .fields
        .get("error")
        .flatMap(
          _.asJsObject
            .fields
            .get("info")
            .map(_.convertTo[String])
        ).getOrElse(deserializationError(s"Can't deserialize the failure response. Given: $json"))

      FailureResponse(errorMsg)
    }
  }
}

object FixerJsonSupport extends FixerJsonSupport
