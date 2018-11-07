package scalac.io.utils

import java.time.{ZoneOffset, ZonedDateTime}

object DateUtils {

  def toUTC(zonedDateTime: ZonedDateTime): ZonedDateTime = zonedDateTime.toInstant.atZone(ZoneOffset.UTC)

}
