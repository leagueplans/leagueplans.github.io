package com.leagueplans.ui.dom.planning.plan.history

import com.leagueplans.common.model.Skill
import com.leagueplans.ui.model.common.forest.Forest.Update
import com.leagueplans.ui.model.common.forest.{Forest, ForestHistory, ForestInterpreter, ForestResolver, Touched}
import com.leagueplans.ui.model.plan.Effect.GainExp
import com.leagueplans.ui.model.plan.{Duration, EffectList, Requirement, Step}
import com.leagueplans.ui.model.player.skill.{Exp, Level}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class StepChangeLabelTest extends AnyFreeSpec with Matchers {
  private val stepA = Step("Kill the Kalphite Queen")
  private val stepB = Step("b")
  private val stepC = Step("c")
  private val stepD = Step("d")

  //  A
  //  ├ B
  //  └ C
  //  D
  private val initial: Forest[Step.ID, Step] =
    Forest.from(
      List(stepA, stepB, stepC, stepD).map(step => step.id -> step).toMap,
      Map(stepA.id -> List(stepB.id, stepC.id)),
      List(stepA.id, stepD.id)
    )

  private def label(change: ForestInterpreter[Step.ID, Step] => List[Update[Step.ID, Step]]): String = {
    val updates = change(ForestInterpreter(initial))
    val updated = ForestResolver.resolve(initial, updates)
    val history = ForestHistory.empty[Step.ID, Step](maxEntries = 1, maxWeight = Int.MaxValue)
      .record(initial, updated, Touched.from(updates))
    StepChangeLabel.describe(history.undoStack.head)
  }

  private def edit(f: Step => Step): String =
    label(_.update(stepA.id, f))

  "StepChangeLabel" - {
    "Deleting a step" in {
      label(_.remove(stepD.id)) shouldEqual "Delete step"
    }

    "Deleting several steps" in {
      label(_.remove(stepA.id)) shouldEqual "Delete 3 steps"
    }

    "Adding a step" in {
      label(_.add(Step("new"), stepA.id)) shouldEqual "Add step"
    }

    "Adding several steps" in {
      val parent = Step("parent")
      label(interpreter =>
        val first = interpreter.add(parent)
        first ++ ForestInterpreter(ForestResolver.resolve(initial, first)).add(Step("child"), parent.id)
      ) shouldEqual "Add 2 steps"
    }

    "Moving a step" in {
      label(_.move(stepD.id, Some(stepB.id))) shouldEqual "Move step"
    }

    "Reordering steps" in {
      label(_.reorder(List(stepC.id, stepB.id))) shouldEqual "Reorder steps"
    }

    "Editing" - {
      "a description" in {
        edit(_.deepCopy(description = "Kill KQ")) shouldEqual "Edit description on \"Kill KQ\""
      }

      "effects" in {
        edit(_.deepCopy(directEffects = EffectList(List(GainExp(Skill.Attack, Exp(10)))))) shouldEqual
          "Edit effects on \"Kill the Kalphite Queen\""
      }

      "requirements" in {
        edit(_.deepCopy(requirements = List(Requirement.SkillLevel(Skill.Attack, Level(10))))) shouldEqual
          "Edit requirements on \"Kill the Kalphite Queen\""
      }

      "repetitions" in {
        edit(_.deepCopy(repetitions = 3)) shouldEqual "Edit repetitions on \"Kill the Kalphite Queen\""
      }

      "a duration" in {
        edit(_.deepCopy(duration = Duration.ticks(10))) shouldEqual "Edit duration on \"Kill the Kalphite Queen\""
      }

      "several fields" in {
        edit(_.deepCopy(repetitions = 3, duration = Duration.ticks(10))) shouldEqual
          "Edit step on \"Kill the Kalphite Queen\""
      }

      "a long description" in {
        edit(_.deepCopy(description = "Travel to the Kalphite Lair and defeat the Kalphite Queen")) shouldEqual
          "Edit description on \"Travel to the Kalphite Lair a…\""
      }

      "several steps" in {
        label(interpreter =>
          interpreter.update(stepB.id, _.deepCopy(repetitions = 2)) ++
            interpreter.update(stepC.id, _.deepCopy(repetitions = 2))
        ) shouldEqual "Edit 2 steps"
      }

      "and moving a step" in {
        label(_.addOption(stepD.deepCopy(repetitions = 2), Some(stepB.id))) shouldEqual "Edit step"
      }
    }
  }
}
