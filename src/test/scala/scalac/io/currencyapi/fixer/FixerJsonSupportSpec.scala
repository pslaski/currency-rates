package scalac.io.currencyapi.fixer

import org.scalatest.{MustMatchers, WordSpec}
import scalac.io.currencyapi.models.CurrencyApiResponses.FailureResponse
import spray.json.{DeserializationException, JsString, _}

class FixerJsonSupportSpec extends WordSpec with MustMatchers {

  private val failureResponseReader = FixerJsonSupport.failureResponseReader

  "failureResponseReader" should {
    "read jsValue as failure response" in {
      val jsonStr =
        s"""
           |{
           |  "success": false,
           |  "error": {
           |    "code": 104,
           |    "info": "Your monthly API request volume has been reached. Please upgrade your plan."
           |  }
           |}
       """.stripMargin

      val json = jsonStr.parseJson

      failureResponseReader.read(json) mustEqual FailureResponse("Your monthly API request volume has been reached. Please upgrade your plan.")
    }

    "throws DeserializationException" when {

      "info field is not a string" in {
        val error = intercept[DeserializationException](
          failureResponseReader.read("""{"success": false, "error": {"code": 104, "info": {}}}""".parseJson)
        )

        error.msg mustEqual """Expected String as JsString, but got {}"""
      }

      "error object doesn't have info field" in {
        val error = intercept[DeserializationException](
          failureResponseReader.read("""{"success": false, "error": {"code": 104}}""".parseJson)
        )

        error.msg mustEqual """Can't deserialize the failure response. Given: {"success":false,"error":{"code":104}}"""
      }

      "error field is not an object" in {
        val error = intercept[DeserializationException](
          failureResponseReader.read("""{"success": false, "error": "message"}""".parseJson)
        )

        error.msg mustEqual "JSON object expected"
      }

      "json doesn't have error field" in {
        val error = intercept[DeserializationException](
          failureResponseReader.read("""{"success": false}""".parseJson)
        )

        error.msg mustEqual """Can't deserialize the failure response. Given: {"success":false}"""
      }

      "json is not an object" in {
        val error = intercept[DeserializationException](
          failureResponseReader.read(JsString("some_string"))
        )

        error.msg mustEqual "JSON object expected"
      }

    }
  }

}
