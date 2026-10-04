package com.leagueplans.ui.dom.planning.drag

import com.leagueplans.common.model.{Item, Skill}
import com.leagueplans.ui.dom.planning.drag.DragSession.Dragged
import com.leagueplans.ui.model.plan.{Effect, EffectList, Requirement, Step, StepDetails}
import com.leagueplans.ui.model.player.item.Depository
import com.leagueplans.ui.model.player.skill.{Exp, Level}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class StepContentTransferTest extends AnyFreeSpec with Matchers {
  private val attackExp = Effect.GainExp(Skill.Attack, Exp(100))
  private val logs = Effect.AddItem(Item.ID(1511), 5, Depository.Kind.Inventory, note = false)
  private val agility50 = Requirement.SkillLevel(Skill.Agility, Level(50))

  private def step(id: String, effects: List[Effect] = List.empty, requirements: List[Requirement] = List.empty): Step =
    Step(Step.ID.fromString(id), StepDetails(id).copy(directEffects = EffectList(effects), requirements = requirements))

  "StepContentTransfer" - {
    "moves an effect to the end of the target's effects" in {
      val source = step("source", effects = List(attackExp, logs))
      val target = step("target", effects = List(Effect.UnlockSkill(Skill.Sailing)))

      val moved = StepContentTransfer(Dragged.DraggedEffect(source.id, 0, attackExp), source, target)

      moved.map(_.source.directEffects.underlying) shouldBe Some(List(logs))
      moved.map(_.target.directEffects.underlying) shouldBe Some(List(Effect.UnlockSkill(Skill.Sailing), attackExp))
    }

    "merges a moved effect into a matching one" in {
      val source = step("source", effects = List(attackExp))
      val target = step("target", effects = List(Effect.GainExp(Skill.Attack, Exp(50))))

      StepContentTransfer(Dragged.DraggedEffect(source.id, 0, attackExp), source, target)
        .map(_.target.directEffects.underlying) shouldBe Some(List(Effect.GainExp(Skill.Attack, Exp(150))))
    }

    "moves a requirement, merging it with the target's requirements" in {
      val source = step("source", requirements = List(agility50))
      val target = step("target", requirements = List(Requirement.SkillLevel(Skill.Agility, Level(30))))

      val moved = StepContentTransfer(Dragged.DraggedRequirement(source.id, 0, agility50), source, target)

      moved.map(_.source.requirements) shouldBe Some(List.empty)
      moved.map(_.target.requirements) shouldBe Some(List(agility50))
    }

    "does nothing if the effect or requirement is no longer where the drag found it" in {
      val source = step("source", effects = List(logs, attackExp))
      val target = step("target")

      StepContentTransfer(Dragged.DraggedEffect(source.id, 0, attackExp), source, target) shouldBe None
      StepContentTransfer(Dragged.DraggedEffect(source.id, 2, attackExp), source, target) shouldBe None
    }
  }
}
