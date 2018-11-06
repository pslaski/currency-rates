package scalac.io.api

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import scalac.io.Models.Currency
import spray.json.{JsString, JsValue, JsonFormat, deserializationError}

import scala.util.Try

trait JsonSupport extends SprayJsonSupport {
  import spray.json.DefaultJsonProtocol._

  implicit val localDateFormat = new JsonFormat[LocalDate] {

    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    override def write(obj: LocalDate): JsValue = JsString(formatter.format(obj))

    override def read(json: JsValue): LocalDate = {
      json match {
        case JsString(localDateString) =>
          Try(LocalDate.parse(localDateString, formatter))
            .getOrElse(deserializationError("ISO offset datetime format expected"))
        case other =>
          deserializationError(s"String value expected. Got: $other")
      }
    }
  }

  implicit val currencyFormat = jsonFormat1(Currency)
}

object JsonSupport extends JsonSupport