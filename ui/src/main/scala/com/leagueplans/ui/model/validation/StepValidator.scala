package com.leagueplans.ui.model.validation

import com.leagueplans.ui.model.EffectResolver
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.ui.model.player.mode.Mode
import com.leagueplans.ui.model.player.{Cache, Player}

object StepValidator {
  def validate(step: Step)(
    player: Player,
    resolver: EffectResolver,
    league: Option[Mode.League],
    cache: Cache
  ): (List[String], Player) = {
    val requirementErrors = RequirementValidator.validate(step.requirements)(player, league, cache)
    val (effectErrors, postEffectsPlayer) = EffectValidator.validate(step.directEffects)(player, resolver, league, cache)
    (requirementErrors ++ effectErrors, postEffectsPlayer)
  }
}
