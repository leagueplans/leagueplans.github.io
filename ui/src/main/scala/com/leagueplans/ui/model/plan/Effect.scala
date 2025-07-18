package com.leagueplans.ui.model.plan

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.common.model.{Item, Skill}
import com.leagueplans.ui.model.player.item.Depository
import com.leagueplans.ui.model.player.skill.Exp

enum Effect {
  case GainExp(skill: Skill, baseExp: Exp)

  case AddItem(item: Item.ID, quantity: Int, target: Depository.Kind, note: Boolean)
  case MoveItem(
    item: Item.ID,
    quantity: Int,
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
  given Encoder[Effect] = Encoder.derived
  given Decoder[Effect] = Decoder.derived
}
