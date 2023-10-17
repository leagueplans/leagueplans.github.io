package ddm.ui.model.player.skill

import io.circe.{Decoder, Encoder}

sealed trait Skill

object Skill {
  case object Agility extends Skill
  case object Attack extends Skill
  case object Construction extends Skill
  case object Cooking extends Skill
  case object Crafting extends Skill
  case object Defence extends Skill
  case object Farming extends Skill
  case object Firemaking extends Skill
  case object Fishing extends Skill
  case object Fletching extends Skill
  case object Herblore extends Skill
  case object Hitpoints extends Skill
  case object Hunter extends Skill
  case object Magic extends Skill
  case object Mining extends Skill
  case object Prayer extends Skill
  case object Ranged extends Skill
  case object Runecraft extends Skill
  case object Slayer extends Skill
  case object Smithing extends Skill
  case object Strength extends Skill
  case object Thieving extends Skill
  case object Woodcutting extends Skill

  val all: List[Skill] =
    List(Agility, Attack, Construction, Cooking, Crafting, Defence, Farming, Firemaking, Fishing,
      Fletching, Herblore, Hitpoints, Hunter, Magic, Mining, Prayer, Ranged, Runecraft, Slayer,
      Smithing, Strength, Thieving, Woodcutting)

  private val nameToSkill: Map[String, Skill] =
    all.map(s => s.toString -> s).toMap

  implicit val encoder: Encoder[Skill] = Encoder[String].contramap(_.toString)
  implicit val decoder: Decoder[Skill] = Decoder[String].emap(s =>
    nameToSkill
      .get(s)
      .toRight(left = s"Unknown skill name: [$s]")
  )
}
