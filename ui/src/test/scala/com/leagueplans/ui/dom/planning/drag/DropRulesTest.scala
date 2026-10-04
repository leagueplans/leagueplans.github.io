package com.leagueplans.ui.dom.planning.drag

import com.leagueplans.common.model.Skill
import com.leagueplans.ui.dom.planning.drag.DragSession.Dragged
import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.{Effect, Requirement, Step}
import com.leagueplans.ui.model.player.skill.{Exp, Level}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class DropRulesTest extends AnyFreeSpec with Matchers {
  private val stepA = Step("a")
  private val stepB = Step("b")
  private val stepC = Step("c")
  private val stepD = Step("d")

  //  A
  //  └ B
  //    └ C
  //  D
  private val forest: Forest[Step.ID, Step] =
    Forest.from(
      List(stepA, stepB, stepC, stepD).map(step => step.id -> step).toMap,
      Map(stepA.id -> List(stepB.id), stepB.id -> List(stepC.id)),
      List(stepA.id, stepD.id)
    )

  private def canDrop(dragged: Dragged, onto: Step): Boolean =
    DropRules.canDrop(dragged, onto.id, forest)

  "a step" - {
    val draggedA = Dragged.DraggedStep(stepA.id)

    "can be dropped on another step" in {
      canDrop(draggedA, stepD) shouldBe true
      canDrop(Dragged.DraggedStep(stepC.id), stepA) shouldBe true
    }

    "can't be dropped on itself" in {
      canDrop(draggedA, stepA) shouldBe false
    }

    "can't be dropped on its own substeps, however deep" in {
      canDrop(draggedA, stepB) shouldBe false
      canDrop(draggedA, stepC) shouldBe false
    }
  }

  "an effect or requirement" - {
    val effect = Dragged.DraggedEffect(stepB.id, 0, Effect.GainExp(Skill.Attack, Exp(100)))
    val requirement = Dragged.DraggedRequirement(stepB.id, 0, Requirement.SkillLevel(Skill.Agility, Level(50)))

    "can be dropped on any other step, including its step's parent and substeps" in {
      List(stepA, stepC, stepD).foreach { onto =>
        canDrop(effect, onto) shouldBe true
        canDrop(requirement, onto) shouldBe true
      }
    }

    "can't be dropped back on its own step" in {
      canDrop(effect, stepB) shouldBe false
      canDrop(requirement, stepB) shouldBe false
    }
  }
}
