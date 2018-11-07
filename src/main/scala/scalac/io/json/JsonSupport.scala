package scalac.io.json

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import scalac.io.Models.Currency
import spray.json.{JsString, JsValue, JsonFormat, deserializationError}

import scala.util.Try

trait JsonSupport {
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

  implicit val currencyFormat = new JsonFormat[Currency] {
    override def write(obj: Currency): JsValue = JsString(obj.symbol)

    override def read(json: JsValue): Currency = Currency(json.convertTo[String])
  }
}

object JsonSupport extends JsonSupport