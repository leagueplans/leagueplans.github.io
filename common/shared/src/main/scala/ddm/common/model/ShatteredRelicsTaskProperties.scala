package ddm.common.model

import ddm.common.model.ShatteredRelicsTaskProperties.Category
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

object ShatteredRelicsTaskProperties {
  sealed trait Category {
    def name: String
  }

  object Category {
    final case class SkillCat(skill: Skill) extends Category { val name: String = "Skill" }
    case object Combat extends Category { val name: String = "Combat" }
    case object Quest extends Category { val name: String = "Quest" }
    case object Clues extends Category { val name: String = "Clues" }
    case object General extends Category { val name: String = "General" }

    implicit val codec: Codec[Category] = deriveCodec
  }

  implicit val codec: Codec[ShatteredRelicsTaskProperties] = deriveCodec
}

final case class ShatteredRelicsTaskProperties(tier: LeagueTaskTier, category: Category)
