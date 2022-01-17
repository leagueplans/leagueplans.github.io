package ddm.ui.model.plan

import ddm.ui.model.player.Quest
import ddm.ui.model.player.item.{Depository, Item}
import ddm.ui.model.player.league.Task
import ddm.ui.model.player.skill.{Exp, Skill}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

sealed trait Effect

object Effect {
  implicit val encoder: Encoder[Effect] = deriveEncoder[Effect]
  implicit val decoder: Decoder[Effect] = deriveDecoder[Effect]

  final case class GainExp(skill: Skill, baseExp: Exp) extends Effect

  final case class GainItem(item: Item.ID, count: Int, target: Depository.ID) extends Effect
  final case class MoveItem(item: Item.ID, count: Int, source: Depository.ID, target: Depository.ID) extends Effect
  final case class DropItem(item: Item.ID, count: Int, source: Depository.ID) extends Effect

  final case class SetMultiplier(multiplier: Int) extends Effect

  final case class CompleteQuest(quest: Quest) extends Effect
  final case class CompleteTask(task: Task) extends Effect
}
