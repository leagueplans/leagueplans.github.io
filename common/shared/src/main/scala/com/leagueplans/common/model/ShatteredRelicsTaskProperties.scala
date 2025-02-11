package com.leagueplans.common.model

import com.leagueplans.common.model.ShatteredRelicsTaskProperties.Category
import io.circe.generic.semiauto.deriveCodec
import io.circe.{Codec, Decoder, Encoder}

import scala.util.{Success, Try}

object ShatteredRelicsTaskProperties {
  enum Category(val name: String) {
    case SkillCat(skill: Skill) extends Category(skill.toString)
    case Combat extends Category("Combat")
    case Quest extends Category("Quest")
    case Clues extends Category ("Clues")
    case General extends Category ("General")
  }

  object Category {
    val values: Array[Category] =
      Array(General, Quest, Clues, Combat) ++ Skill.values.map(SkillCat.apply)

    given Encoder[Category] =
      Encoder[String].contramap(_.name)

    given Decoder[Category] =
      Decoder[String].emapTry {
        case "Combat" => Success(Combat)
        case "Quest" => Success(Quest)
        case "Clues" => Success(Clues)
        case "General" => Success(General)
        case other => Try(Skill.valueOf(other)).map(SkillCat.apply)
      }
  }

  given Codec[ShatteredRelicsTaskProperties] = deriveCodec
}

final case class ShatteredRelicsTaskProperties(tier: LeagueTaskTier, category: Category)
