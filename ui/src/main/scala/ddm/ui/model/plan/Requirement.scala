package ddm.ui.model.plan

import ddm.codec.decoding.Decoder
import ddm.codec.encoding.Encoder
import ddm.common.model.{Item, Skill}
import ddm.ui.model.player.item.Depository
import ddm.ui.model.player.skill.Level

enum Requirement {
  case SkillLevel(skill: Skill, level: Level)
  case Tool(item: Item.ID, location: Depository.Kind)
  case And(left: Requirement, right: Requirement)
  case Or(left: Requirement, right: Requirement)
}

object Requirement {
  given Encoder[Requirement] = Encoder.derived
  given Decoder[Requirement] = Decoder.derived
}
