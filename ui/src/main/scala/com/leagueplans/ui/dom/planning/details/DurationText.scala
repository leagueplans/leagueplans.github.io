package com.leagueplans.ui.dom.planning.details

import com.leagueplans.ui.model.plan.Duration

import scala.concurrent.duration.FiniteDuration

/** Reads and writes step durations as typed into the duration chip, such as "2m30s", "90s",
  * "1h 5m" or "50t". A bare number is a number of seconds. Ticks can't be mixed with other units,
  * since a step's duration is stored as either seconds or ticks.
  */
object DurationText {
  val maxSeconds: Int = 21600
  val maxTicks: Int = 36000

  private val tickPattern = """(\d+)\s*(?:t|ticks?)""".r
  private val partPattern = """(\d+)\s*(h|m|s)""".r

  def parse(text: String): Either[String, Duration] = {
    val trimmed = text.trim.toLowerCase
    trimmed match {
      case "" | "0" => Right(Duration.seconds(0))
      case tickPattern(ticks) => toTicks(ticks)
      case bare if bare.forall(_.isDigit) => toSeconds(BigInt(bare))
      case _ => parseParts(trimmed)
    }
  }

  private def parseParts(text: String): Either[String, Duration] = {
    val parts = partPattern.findAllMatchIn(text).toList
    val unmatched = partPattern.replaceAllIn(text, "").trim
    val units = parts.map(_.group(2))
    if (parts.isEmpty || unmatched.nonEmpty || units.distinct.size != units.size)
      Left("Type a duration such as 2m30s, 90s or 50t")
    else
      toSeconds(parts.map(part => BigInt(part.group(1)) * unitSeconds(part.group(2))).sum)
  }

  private def unitSeconds(unit: String): Int =
    unit match {
      case "h" => 3600
      case "m" => 60
      case _ => 1
    }

  private def toSeconds(seconds: BigInt): Either[String, Duration] =
    if (seconds > maxSeconds)
      Left(s"Break this up with a \"log back in\" step (max ${format(Duration.seconds(maxSeconds))})")
    else
      Right(Duration.seconds(seconds.toInt))

  private def toTicks(ticks: String): Either[String, Duration] = {
    val count = BigInt(ticks)
    if (count > maxTicks)
      Left(s"Break this up with a \"log back in\" step (max $maxTicks ticks)")
    else
      Right(Duration.ticks(count.toInt))
  }

  /** Formats a span of time in the plan, such as when a step starts, e.g. "1h 05m 00s" */
  def formatElapsed(duration: scala.concurrent.duration.Duration): String =
    duration match {
      case infinite: scala.concurrent.duration.Duration.Infinite =>
        if (infinite >= scala.concurrent.duration.Duration.Zero) "∞" else "-∞"
      case finite: FiniteDuration =>
        List(
          finite.toDays -> 'd',
          finite.toHours % 24 -> 'h',
          finite.toMinutes % 60 -> 'm',
          finite.toSeconds % 60 -> 's'
        ).dropWhile(_._1 == 0) match {
          case Nil => "00s"
          case (length, token) :: tail =>
            tail.foldLeft(s"$length$token") { case (acc, (length, token)) =>
              s"$acc ${String.format("%02d", length)}$token"
            }
        }
    }

  /** Formats a duration so that parsing the result gives the same duration back */
  def format(duration: Duration): String =
    duration.unit match {
      case Duration.Unit.Ticks => s"${duration.length}t"
      case Duration.Unit.Seconds =>
        val hours = duration.length / 3600
        val minutes = duration.length % 3600 / 60
        val seconds = duration.length % 60
        List(hours -> "h", minutes -> "m", seconds -> "s")
          .filter(_._1 != 0)
          .map((n, unit) => s"$n$unit")
          .mkString(" ") match {
            case "" => "0s"
            case formatted => formatted
          }
    }
}
