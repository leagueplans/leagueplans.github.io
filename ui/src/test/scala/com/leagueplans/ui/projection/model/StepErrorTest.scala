package com.leagueplans.ui.projection.model

import com.leagueplans.common.model.Skill
import com.leagueplans.ui.model.plan.{Effect, EffectList, Requirement, Step, StepDetails}
import com.leagueplans.ui.model.player.skill.{Exp, Level}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class StepErrorTest extends AnyFreeSpec with Matchers {
  private val attackExp = Effect.GainExp(Skill.Attack, 1, Exp(100))
  private val cooksAssistant = Effect.CompleteQuest(1)
  private val agility50 = Requirement.SkillLevel(Skill.Agility, Level(50))

  private def step(effects: List[Effect], requirements: List[Requirement] = List.empty): Step =
    Step(
      Step.ID.fromString("step"),
      StepDetails("step").copy(directEffects = EffectList(effects), requirements = requirements)
    )

  "A problem's source" - {
    "is current while the step holds its effect or requirement at the same position" in {
      val current = step(List(attackExp, cooksAssistant), List(agility50))
      StepError.Source.Effect(1, cooksAssistant).isCurrentFor(current) shouldBe true
      StepError.Source.Requirement(0, agility50).isCurrentFor(current) shouldBe true
    }

    "tells apart equal effects by their position" in {
      val current = step(List(cooksAssistant, attackExp))
      StepError.Source.Effect(0, cooksAssistant).isCurrentFor(current) shouldBe true
      StepError.Source.Effect(1, cooksAssistant).isCurrentFor(current) shouldBe false
    }

    "is stale once something else is at its position" in {
      // The first effect was deleted, so the second has moved up
      val afterDelete = step(List(cooksAssistant))
      StepError.Source.Effect(0, attackExp).isCurrentFor(afterDelete) shouldBe false
    }

    "is stale once its effect has been edited" in {
      val afterEdit = step(List(Effect.GainExp(Skill.Attack, 1, Exp(200))))
      StepError.Source.Effect(0, attackExp).isCurrentFor(afterEdit) shouldBe false
    }

    "is stale once its position is gone" in {
      StepError.Source.Requirement(0, agility50).isCurrentFor(step(List.empty)) shouldBe false
    }
  }
}
