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

  /** Adds a requirement to the end of a list, merging it with the list's requirements where it
    * can. A requirement already in the list isn't added again, and a skill level replaces a lower
    * level for the same skill, since both would have to be met. */
  def addTo(requirements: List[Requirement], requirement: Requirement): List[Requirement] =
    requirement match {
      case _ if requirements.contains(requirement) =>
        requirements
      case SkillLevel(skill, level) =>
        requirements.indexWhere {
          case SkillLevel(`skill`, _) => true
          case _ => false
        } match {
          case -1 => requirements :+ requirement
          case i =>
            requirements(i) match {
              case SkillLevel(_, existing) if existing.raw >= level.raw => requirements
              case _ => requirements.updated(i, requirement)
            }
        }
      case _ =>
        requirements :+ requirement
    }
}
