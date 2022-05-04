package ddm.ui

import ddm.common.model.Item
import ddm.ui.model.player.skill.Skill

object ResourcePaths {
  def skillIcon(skill: Skill): String =
    s"images/skill-icons/${skill.toString}.png"

  def itemIcon(item: Item, count: Int): String =
    s"images/items/${item.imageFor(count).raw}"

  val itemsJson: String =
    "data/items.json"

  val defaultPlanJson: String =
    "data/plan.json"

  val planStorageKey: String =
    "plan"
}
