package com.leagueplans.ui.model.plan

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.ui.model.player.skill.{Exp, Level}

/** The level, or amount of exp, that a `GainExpToTarget` effect takes a skill to */
enum ExpTarget {
  case AtLevel(level: Level)
  case AtExp(exp: Exp)

  /** The exp the target comes to */
  def goal: Exp =
    this match {
      case AtLevel(level) => level.bound
      case AtExp(exp) => exp
    }
}

object ExpTarget {
  given Encoder[ExpTarget] = Encoder.derived
  given Decoder[ExpTarget] = Decoder.derived
}
