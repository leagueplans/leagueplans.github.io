package ddm.ui.model.plan

import ddm.common.model.{Item, Skill}
import ddm.ui.model.player.item.Depository
import ddm.ui.model.player.skill.Exp
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

sealed trait Effect

object Effect {
  implicit val codec: Codec[Effect] = deriveCodec[Effect]

  final case class GainExp(skill: Skill, baseExp: Exp) extends Effect

  final case class AddItem(item: Item.ID, count: Int, target: Depository.Kind, note: Boolean) extends Effect
  final case class MoveItem(
    item: Item.ID,
    count: Int,
    source: Depository.Kind,
    notedInSource: Boolean,
    target: Depository.Kind,
    noteInTarget: Boolean
  ) extends Effect

  final case class UnlockSkill(skill: Skill) extends Effect
  final case class SetMultiplier(multiplier: Int) extends Effect

  final case class CompleteQuest(quest: Int) extends Effect
  final case class CompleteDiaryTask(task: Int) extends Effect
  final case class CompleteLeagueTask(task: Int) extends Effect
}
