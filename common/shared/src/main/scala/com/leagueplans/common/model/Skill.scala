package com.leagueplans.common.model

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import io.circe.{Decoder as JsonDecoder, Encoder as JsonEncoder}

import scala.util.Try

enum Skill {
  case Agility, Attack, Construction, Cooking, Crafting, Defence, Farming,
  Firemaking, Fishing, Fletching, Herblore, Hitpoints, Hunter, Magic, Mining,
  Prayer, Ranged, Runecraft, Slayer, Smithing, Strength, Thieving, Woodcutting
}

object Skill {
  given JsonEncoder[Skill] = JsonEncoder[String].contramap(_.toString)
  given JsonDecoder[Skill] = JsonDecoder[String].emapTry(s =>
    Try(Skill.valueOf(s))
  )

  given Encoder[Skill] = Encoder.derived
  given Decoder[Skill] = Decoder.derived
}
