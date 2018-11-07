package scalac.io.utils

import java.time.ZonedDateTime

import akka.http.scaladsl.unmarshalling.{FromStringUnmarshaller, Unmarshaller}
import scalac.io.Models.Currency

object Unmarshallers {

  implicit val stringToZonedDateTime: FromStringUnmarshaller[ZonedDateTime] = Unmarshaller.strict(ZonedDateTime.parse)

  implicit val stringToCurrency: FromStringUnmarshaller[Currency] = Unmarshaller.strict(Currency)
}
