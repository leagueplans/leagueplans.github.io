package com.leagueplans.ui.model.plan

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.common.model.{Item, Skill}
import com.leagueplans.ui.model.player.skill.Level

enum Requirement {
  case SkillLevel(skill: Skill, level: Level)
  /** The player holds one of an item, such as a pickaxe for mining */
  case Holds(item: Item.ID, where: Requirement.Where)
  case And(left: Requirement, right: Requirement)
  case Or(left: Requirement, right: Requirement)
}

object Requirement {
  given Encoder[Requirement] = Encoder.derived
  given Decoder[Requirement] = Decoder.derived

  /** Where a required item can be held. Equipped means in the item's own slot. */
  enum Where {
    case Inventory, Equipped, InventoryOrEquipped

    /** Such as "in the inventory or worn" */
    def description: String =
      this match {
        case Inventory => "in the inventory"
        case Equipped => "worn"
        case InventoryOrEquipped => "in the inventory or worn"
      }
  }

  object Where {
    given Encoder[Where] = Encoder.derived
    given Decoder[Where] = Decoder.derived
  }

  /** Requires an item in the inventory, or equipped as well if it can be equipped */
  def held(item: Item): Holds =
    Holds(item.id, if (item.equipmentType.isEmpty) Where.Inventory else Where.InventoryOrEquipped)

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
