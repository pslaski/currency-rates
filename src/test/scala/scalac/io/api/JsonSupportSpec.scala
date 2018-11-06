package scalac.io.api

import java.time.LocalDate

import org.scalatest.{MustMatchers, WordSpec}
import spray.json.{DeserializationException, JsNull, JsString}

class JsonSupportSpec extends WordSpec with MustMatchers {

  private val localDateFormat = JsonSupport.localDateFormat

  "localDateFormat" should {
    val localDateString = "2016-05-01"
    val localDate = LocalDate.parse(localDateString)

    "write local date as JsValue" in {
      localDateFormat.write(localDate) mustEqual JsString("2016-05-01")
    }

    "read JsValue as local date" in {
      localDateFormat.read(JsString(localDateString)) mustEqual localDate
    }

    "throws DeserializationException" when {

      "given string can't be parsed to local date" in {
        val error = intercept[DeserializationException](
          localDateFormat.read(JsString("not_local_date"))
        )

        error.msg mustEqual "ISO offset datetime format expected"
      }

      "JsValue is not a JsString" in {
        val error = intercept[DeserializationException](
          localDateFormat.read(JsNull)
        )

        error.msg mustEqual "String value expected. Got: null"
      }
    }
  }

}
