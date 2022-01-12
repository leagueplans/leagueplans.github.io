package ddm.ui.component

import ddm.ui.model.player.item.Item
import ddm.ui.model.player.skill.Skill

object ResourcePaths {
  def skillIcon(skill: Skill): String =
    s"images/skill-icons/${skill.toString}.png"

  def itemIcon(id: Item.ID): String =
    s"images/items/${id.raw}.png"

  val itemsJson: String =
    "data/items.json"
}
