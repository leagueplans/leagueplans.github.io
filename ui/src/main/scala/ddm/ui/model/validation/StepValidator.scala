package ddm.ui.model.validation

import ddm.ui.model.plan.Step
import ddm.ui.model.player.Player
import ddm.ui.model.player.item.ItemCache

object StepValidator {
  def validate(step: Step)(player: Player, itemCache: ItemCache): List[String] =
    RequirementValidator.validate(step.requirements)(player, itemCache) ++
      EffectValidator.validate(step.directEffects)(player, itemCache)
}
