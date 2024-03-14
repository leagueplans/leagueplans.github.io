package ddm.ui.model.plan

import ddm.common.model.{Item, Skill}
import ddm.ui.model.player.item.Depository
import ddm.ui.model.player.skill.Level
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

enum Requirement {
  case SkillLevel(skill: Skill, level: Level)
  case Tool(item: Item.ID, location: Depository.Kind)
  case And(left: Requirement, right: Requirement)
  case Or(left: Requirement, right: Requirement)
}

object Requirement {
  given Codec[Requirement] = deriveCodec[Requirement]
}
