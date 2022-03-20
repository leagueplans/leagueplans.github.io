package ddm.ui

import ddm.common.model.Item
import ddm.ui.model.player.skill.Skill

object ResourcePaths {
  def skillIcon(skill: Skill): String =
    s"images/skill-icons/${skill.toString}.png"

  def itemIcon(id: Item.ID): String =
    s"images/items/${id.raw}.png"

  val itemsJson: String =
    "data/items.json"

  val defaultPlanJson: String =
    "data/plan.json"

  val planStorageKey: String =
    "plan"
}
