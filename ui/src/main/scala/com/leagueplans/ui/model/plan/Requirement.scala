package com.leagueplans.ui.model.plan

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.common.model.{Item, Skill}
import com.leagueplans.ui.model.player.item.Depository
import com.leagueplans.ui.model.player.skill.Level

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
