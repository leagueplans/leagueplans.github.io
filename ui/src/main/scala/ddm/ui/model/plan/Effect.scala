package ddm.ui.model.plan

import ddm.ui.model.player.item.{Depository, Item}
import ddm.ui.model.player.league.Task
import ddm.ui.model.player.skill.{Exp, Skill}

sealed trait Effect

object Effect {
  final case class GainExp(skill: Skill, baseExp: Exp) extends Effect

  final case class GainItem(item: Item.ID, count: Int, target: Depository.ID) extends Effect
  final case class MoveItem(item: Item.ID, count: Int, source: Depository.ID, target: Depository.ID) extends Effect
  final case class DestroyItem(item: Item.ID, count: Int, source: Depository.ID) extends Effect

  final case class GainQuestPoints(count: Int) extends Effect

  final case class SetMultiplier(multiplier: Int) extends Effect

  final case class CompleteTask(task: Task) extends Effect
}
