package com.leagueplans.ui.model.plan

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder

/** How many of an item a move takes. `Max` is worked out each time the effect applies, so an
  * earlier change to the plan, such as spending less on supplies, carries through to later effects
  * without editing them.
  */
enum ItemQuantity {
  case Exact(count: Int)
  /** As many as can be: all that are held, though a withdrawal stops at what fits in the inventory */
  case Max
}

object ItemQuantity {
  given Encoder[ItemQuantity] = Encoder.derived
  given Decoder[ItemQuantity] = Decoder.derived

  enum Problem {
    case Unreadable, BelowOne, AboveMax
  }

  /** A typed amount: "max" or "all" for the most, or a count as the bank's amount box takes it */
  def parse(text: String): Either[Problem, ItemQuantity] =
    text.trim match {
      case word if word.equalsIgnoreCase("max") || word.equalsIgnoreCase("all") => Right(Max)
      case other => parseCount(other).map(Exact(_))
    }

  /** A count with the game's bank shortcuts: "k", "m" or "b" after a number for thousands,
    * millions or billions, as in "10k". Unlike in game, the number can have a decimal part, as in
    * "1.5k", and a count that comes out between whole numbers is rounded down. Commas are ignored,
    * and the letters don't combine. */
  def parseCount(text: String): Either[Problem, Int] =
    text.trim.replace(",", "").stripPrefix("+") match {
      case countPattern(number, suffix) =>
        val count = (BigDecimal(number) * multipliers(Option(suffix).fold(' ')(_.head.toLower)))
          .setScale(0, BigDecimal.RoundingMode.FLOOR)
        if (count < 1) Left(Problem.BelowOne)
        else if (count > Int.MaxValue) Left(Problem.AboveMax)
        else Right(count.toInt)
      case other if other.startsWith("-") && countPattern.matches(other.drop(1)) => Left(Problem.BelowOne)
      case _ => Left(Problem.Unreadable)
    }

  private val countPattern = """(\d+\.?\d*|\.\d+)([kKmMbB])?""".r

  private val multipliers: Map[Char, BigDecimal] =
    Map(' ' -> 1, 'k' -> 1000, 'm' -> 1000000, 'b' -> 1000000000)
}
