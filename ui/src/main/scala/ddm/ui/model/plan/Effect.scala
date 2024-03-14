package ddm.ui.model.plan

import ddm.common.model.{Item, Skill}
import ddm.ui.model.player.item.Depository
import ddm.ui.model.player.skill.Exp
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

enum Effect {
  case GainExp(skill: Skill, baseExp: Exp)

  case AddItem(item: Item.ID, count: Int, target: Depository.Kind, note: Boolean)
  case MoveItem(
    item: Item.ID,
    count: Int,
    source: Depository.Kind,
    notedInSource: Boolean,
    target: Depository.Kind,
    noteInTarget: Boolean
  )

  case UnlockSkill(skill: Skill)

  case CompleteQuest(quest: Int)
  case CompleteDiaryTask(task: Int)
  case CompleteLeagueTask(task: Int)
}

object Effect {
  given Codec[Effect] = deriveCodec[Effect]
}
