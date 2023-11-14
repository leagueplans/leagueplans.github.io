package ddm.ui.model.validation

import ddm.ui.model.plan.Step
import ddm.ui.model.player.{Cache, Player}

object StepValidator {
  def validate(step: Step)(player: Player, cache: Cache): (List[String], Player) = {
    val requirementErrors = RequirementValidator.validate(step.requirements)(player, cache)
    val (effectErrors, postEffectsPlayer) = EffectValidator.validate(step.directEffects)(player, cache)
    (requirementErrors ++ effectErrors, postEffectsPlayer)
  }
}
