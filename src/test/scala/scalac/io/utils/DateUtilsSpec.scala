package scalac.io.utils

import java.time.ZonedDateTime

import org.scalatest.{MustMatchers, WordSpec}

class DateUtilsSpec extends WordSpec with MustMatchers {

  "DateUtils.toUTC" should {
    "normalize zonedDateTime to UTC zone" in {
      val timestampWithZone = "2016-05-01T00:34:46+01:00"
      val zonedDateTimeWithZone = ZonedDateTime.parse(timestampWithZone)

      val zonedDateTimeUTC = DateUtils.toUTC(zonedDateTimeWithZone)

      val expectedZonedDateTime = ZonedDateTime.parse("2016-04-30T23:34:46Z")

      zonedDateTimeUTC mustBe expectedZonedDateTime
    }
  }

}
