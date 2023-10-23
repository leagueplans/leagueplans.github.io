package ddm.ui.model.plan

import ddm.common.model.Item
import ddm.ui.model.player.item.Depository
import ddm.ui.model.player.skill.Skill
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

sealed trait Requirement

object Requirement {
  implicit val codec: Codec[Requirement] = deriveCodec[Requirement]

  final case class Level(skill: Skill, level: Int) extends Requirement
  final case class Tool(item: Item.ID, location: Depository.Kind) extends Requirement

  final case class And(left: Requirement, right: Requirement) extends Requirement
  final case class Or(left: Requirement, right: Requirement) extends Requirement
}
