package com.leagueplans.ui.dom.planning.details.repeat

import cats.data.NonEmptyList
import com.leagueplans.common.model.{InfoboxKey, Item, Skill}
import com.leagueplans.ui.dom.planning.details.repeat.LoopSummary.Result
import com.leagueplans.ui.model.plan.StopCondition.{Comparison, ItemForms, ItemLocation}
import com.leagueplans.ui.model.plan.{ExpTarget, Repetitions, StopCondition}
import com.leagueplans.ui.model.player.skill.Level
import com.leagueplans.ui.projection.model.{LoopRun, StepLoop}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class LoopSummaryTest extends AnyFreeSpec with Matchers {
  "LoopSummary" - {
    val feathers = Item(
      Item.ID(314),
      gameID = None,
      "Feather",
      examine = "",
      NonEmptyList.one((Item.Image.Bin(1), Item.Image.Path("1/1.png"))),
      Item.Bankable.Yes(stacks = true),
      stackable = true,
      noteable = false,
      equipmentType = None,
      infobox = InfoboxKey(314, List.empty)
    )
    val items: Item.ID => Item = Map(feathers.id -> feathers)

    val hunter60 = StopCondition.SkillReaches(Skill.Hunter, ExpTarget.AtLevel(Level.L60), stopBefore = false)
    val noFeathers =
      StopCondition.ItemCount(feathers.id, ItemLocation.Inventory, ItemForms.Unnoted, Comparison.AtMost, 0, stopBefore = true)

    def run(reps: Int, outcome: LoopRun.Outcome): LoopRun =
      LoopRun(reps, outcome, List.empty)

    def result(condition: StopCondition, passes: LoopRun*): Result =
      LoopSummary.result(StepLoop(Repetitions.Until(condition, None), passes.toList), items)

    "count" - {
      "is how many times a step ran" in {
        LoopSummary.count(List(run(1412, LoopRun.Outcome.Met(List(hunter60))))) shouldBe "1,412"
      }

      "is a range when the times the plan reaches the step differ" in {
        LoopSummary.count(List(27, 24, 21).map(run(_, LoopRun.Outcome.Met(List.empty)))) shouldBe "21–27"
      }
    }

    "tooltip" - {
      "names the times and the condition" in {
        LoopSummary.tooltip(hunter60, Some(List(run(412, LoopRun.Outcome.Met(List(hunter60))))), items) shouldBe
          "Runs 412 times, until Hunter reaches level 60"
      }

      "says once for a single time" in {
        LoopSummary.tooltip(hunter60, Some(List(run(1, LoopRun.Outcome.Met(List(hunter60))))), items) shouldBe
          "Runs once, until Hunter reaches level 60"
      }

      "gives the range when the plan reaches the step more than once" in {
        LoopSummary.tooltip(hunter60, Some(List(27, 21).map(run(_, LoopRun.Outcome.Met(List(hunter60))))), items) shouldBe
          "Runs 21–27 times each time the plan reaches it, until Hunter reaches level 60"
      }

      "says once when it runs once each time the plan reaches the step" in {
        LoopSummary.tooltip(hunter60, Some(List(1, 1).map(run(_, LoopRun.Outcome.Met(List(hunter60))))), items) shouldBe
          "Runs once each time the plan reaches it, until Hunter reaches level 60"
      }

      "says the times are being worked out before they're known" in {
        LoopSummary.tooltip(hunter60, None, items) shouldBe
          "Repeats until Hunter reaches level 60. Working out how many times that takes…"
      }
    }

    "result" - {
      "says how many times a loop that met its condition ran" in {
        result(hunter60, run(412, LoopRun.Outcome.Met(List(hunter60)))) shouldBe Result.Stops("Stops after running 412 times")
      }

      "says once for a single time" in {
        result(hunter60, run(1, LoopRun.Outcome.Met(List(hunter60)))) shouldBe Result.Stops("Stops after running once")
      }

      "names the condition that stopped a loop with any of several" in {
        result(StopCondition.Or(hunter60, noFeathers), run(412, LoopRun.Outcome.Met(List(hunter60)))) shouldBe
          Result.Stops("Stops after running 412 times: Hunter reaches level 60 first")
      }

      "says what a loop stopped before" in {
        result(noFeathers, run(600, LoopRun.Outcome.StoppedBefore(List(noFeathers)))) shouldBe
          Result.Stops("Stops after running 600 times, before Feather runs out")
      }

      "names the step's own limit" in {
        result(hunter60, run(300, LoopRun.Outcome.ReachedMax(300))) shouldBe Result.Stops("Stops at its limit of 300 times")
      }

      "lists a few times the plan reaches the step" in {
        result(hunter60, List(27, 24, 21).map(run(_, LoopRun.Outcome.Met(List(hunter60))))*) shouldBe
          Result.Stops("Runs 27, 24 and 21 times, on the 3 times the plan reaches it")
      }

      "says once when a few times the plan reaches the step it runs once each" in {
        result(hunter60, List(1, 1, 1).map(run(_, LoopRun.Outcome.Met(List(hunter60))))*) shouldBe
          Result.Stops("Runs once, on the 3 times the plan reaches it")
      }

      "gives the range across many times the plan reaches the step" in {
        result(hunter60, List(5, 6, 7, 8, 9).map(run(_, LoopRun.Outcome.Met(List(hunter60))))*) shouldBe
          Result.Stops("Runs 5–9 times, on each of the 5 times the plan reaches it")
      }

      "says once when many times the plan reaches the step it runs once each" in {
        result(hunter60, List.fill(6)(run(1, LoopRun.Outcome.Met(List(hunter60))))*) shouldBe
          Result.Stops("Runs once, on each of the 6 times the plan reaches it")
      }

      "gives a loop's problems" in {
        result(hunter60, run(0, LoopRun.Outcome.AlreadyMet(List(hunter60)))) shouldBe
          Result.Problems(List("Hunter is already level 60, so this step doesn't run"))
      }
    }
  }
}
