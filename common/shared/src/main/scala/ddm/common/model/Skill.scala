package ddm.common.model

import io.circe.{Decoder, Encoder}

import scala.util.Try

enum Skill {
  case Agility, Attack, Construction, Cooking, Crafting, Defence, Farming,
  Firemaking, Fishing, Fletching, Herblore, Hitpoints, Hunter, Magic, Mining,
  Prayer, Ranged, Runecraft, Slayer, Smithing, Strength, Thieving, Woodcutting
}

object Skill {
  given Encoder[Skill] = Encoder[String].contramap(_.toString)
  given Decoder[Skill] = Decoder[String].emapTry(s =>
    Try(Skill.valueOf(s))
  )
}
