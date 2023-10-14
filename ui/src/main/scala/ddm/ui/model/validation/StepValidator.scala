package ddm.ui.model.validation

import ddm.ui.model.plan.Step
import ddm.ui.model.player.{Cache, Player}

object StepValidator {
  def validate(step: Step)(player: Player, cache: Cache): List[String] =
    RequirementValidator.validate(step.requirements)(player, cache) ++
      EffectValidator.validate(step.directEffects)(player, cache)
}
