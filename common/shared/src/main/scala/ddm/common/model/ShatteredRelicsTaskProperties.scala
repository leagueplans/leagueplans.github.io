package ddm.common.model

import ddm.common.model.ShatteredRelicsTaskProperties.Category
import io.circe.{Codec, Decoder, Encoder, Json}
import io.circe.generic.semiauto.deriveCodec

import scala.util.Success

object ShatteredRelicsTaskProperties {
  sealed trait Category {
    def name: String
  }

  object Category {
    final case class SkillCat(skill: Skill) extends Category { val name: String = skill.toString }
    case object Combat extends Category { val name: String = "Combat" }
    case object Quest extends Category { val name: String = "Quest" }
    case object Clues extends Category { val name: String = "Clues" }
    case object General extends Category { val name: String = "General" }

    implicit val encoder: Encoder[Category] =
      Encoder[String].contramap(_.name)

    implicit val decoder: Decoder[Category] =
      Decoder[String].emapTry {
        case "Combat" => Success(Combat)
        case "Quest" => Success(Quest)
        case "Clues" => Success(Clues)
        case "General" => Success(General)
        case other =>
          Decoder[Skill]
            .decodeJson(Json.fromString(other))
            .map(SkillCat)
            .toTry
      }
  }

  implicit val codec: Codec[ShatteredRelicsTaskProperties] = deriveCodec
}

final case class ShatteredRelicsTaskProperties(tier: LeagueTaskTier, category: Category)
