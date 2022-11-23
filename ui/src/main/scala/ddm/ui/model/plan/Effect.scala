package ddm.ui.model.plan

import ddm.common.model.Item
import ddm.ui.model.player.Quest
import ddm.ui.model.player.item.Depository
import ddm.ui.model.player.league.Task
import ddm.ui.model.player.skill.{Exp, Skill}
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

sealed trait Effect

object Effect {
  implicit val codec: Codec[Effect] = deriveCodec[Effect]

  final case class GainExp(skill: Skill, baseExp: Exp) extends Effect

  final case class GainItem(item: Item.ID, count: Int, target: Depository.ID) extends Effect
  final case class MoveItem(item: Item.ID, count: Int, source: Depository.ID, target: Depository.ID) extends Effect

  final case class UnlockSkill(skill: Skill) extends Effect
  final case class SetMultiplier(multiplier: Int) extends Effect

  final case class CompleteQuest(quest: Quest) extends Effect
  final case class CompleteTask(task: Task) extends Effect
}
