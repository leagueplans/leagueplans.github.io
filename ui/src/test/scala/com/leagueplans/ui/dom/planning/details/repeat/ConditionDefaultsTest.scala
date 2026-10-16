package com.leagueplans.ui.dom.planning.details.repeat

import cats.data.NonEmptyList
import com.leagueplans.common.model.{InfoboxKey, Item, Skill}
import com.leagueplans.ui.dom.planning.details.repeat.ConditionDefaults.{OwnItem, Start, Use}
import com.leagueplans.ui.model.plan.StopCondition.{Comparison, ItemForms, ItemLocation}
import com.leagueplans.ui.model.plan.*
import com.leagueplans.ui.model.player.item.Depository
import com.leagueplans.ui.model.player.league.LeagueStatus
import com.leagueplans.ui.model.player.skill.{Exp, Level, Stats}
import com.leagueplans.ui.model.player.{GridStatus, Player}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class ConditionDefaultsTest extends AnyFreeSpec with Matchers {
  "ConditionDefaults" - {
    def item(id: Int, name: String, stackable: Boolean): Item =
      Item(
        Item.ID(id),
        gameID = None,
        name,
        examine = "",
        NonEmptyList.one((Item.Image.Bin(1), Item.Image.Path("1/1.png"))),
        Item.Bankable.Yes(stacks = true),
        stackable = stackable,
        noteable = true,
        equipmentType = None,
        infobox = InfoboxKey(id, List.empty)
      )

    val feathers = item(314, "Feather", stackable = true)
    val salamander = item(10149, "Red salamander", stackable = false)
    val rope = item(954, "Rope", stackable = false)
    val items: Item.ID => Item = Map(feathers.id -> feathers, salamander.id -> salamander, rope.id -> rope)

    val player =
      Player(Stats(), Map.empty, Set.empty, Set.empty, LeagueStatus(0, Set.empty, Set.empty), GridStatus(Set.empty))
        .copy(depositories = Map(Depository.Kind.Inventory -> Depository(Map((rope.id, false) -> 1, (feathers.id, false) -> 600), Depository.Kind.Inventory)))

    def step(effects: Effect*): Step =
      Step(Step.ID.fromString("step"), StepDetails("step").copy(directEffects = EffectList(effects.toList)))

    "own skills are those the step's effects give xp in, in order" in {
      val s = step(
        Effect.GainExp(Skill.Hunter, 1, Exp(304)),
        Effect.GainExpToTarget(Skill.Agility, ExpTarget.AtLevel(Level.L40), None),
        Effect.GainExp(Skill.Hunter, 1, Exp(1))
      )
      ConditionDefaults.ownSkills(s) shouldBe List(Skill.Hunter, Skill.Agility)
    }

    "own items" - {
      "say whether the step adds or removes them, where, and in which form" in {
        val s = step(
          Effect.AddItem(feathers.id, ItemChange.By(-1), Depository.Kind.Inventory, note = false),
          Effect.AddItem(salamander.id, ItemChange.By(1), Depository.Kind.Bank, note = true)
        )
        ConditionDefaults.ownItems(s) shouldBe List(
          OwnItem(feathers.id, Use.Removes, ItemLocation.Inventory, ItemForms.Unnoted),
          OwnItem(salamander.id, Use.Adds, ItemLocation.Bank, ItemForms.Noted)
        )
      }

      "count a move into the inventory as adding, and one out of it as removing" in {
        val s = step(
          Effect.MoveItem(rope.id, ItemQuantity.Exact(1), Depository.Kind.Bank, false, Depository.Kind.Inventory, false),
          Effect.MoveItem(feathers.id, ItemQuantity.Exact(1), Depository.Kind.Inventory, false, Depository.Kind.Bank, false)
        )
        ConditionDefaults.ownItems(s).map(own => (own.item, own.use)) shouldBe
          List(rope.id -> Use.Adds, feathers.id -> Use.Removes)
      }

      "are as the first effect naming them has them" in {
        val s = step(
          Effect.AddItem(rope.id, ItemChange.By(1), Depository.Kind.Inventory, note = false),
          Effect.AddItem(rope.id, ItemChange.By(-1), Depository.Kind.Inventory, note = false)
        )
        ConditionDefaults.ownItems(s).map(_.use) shouldBe List(Use.Adds)
      }
    }

    "a skill starts as a draft for the next level" in {
      ConditionDefaults.skill(Skill.Hunter, player) shouldBe
        Start(StopCondition.SkillReaches(Skill.Hunter, ExpTarget.AtLevel(Level.L2), stopBefore = false), complete = false)
    }

    "an item the step removes is complete: none left in its place, stopping before passing it" in {
      val own = OwnItem(feathers.id, Use.Removes, ItemLocation.Inventory, ItemForms.Unnoted)
      ConditionDefaults.item(feathers, Some(own), player, items) shouldBe Start(
        StopCondition.ItemCount(feathers.id, ItemLocation.Inventory, ItemForms.Unnoted, Comparison.AtMost, 0, stopBefore = true),
        complete = true
      )
    }

    "an item the step adds is a draft for at least what fills the inventory" in {
      // 2 slots are used, so 26 are free
      val own = OwnItem(salamander.id, Use.Adds, ItemLocation.Inventory, ItemForms.Unnoted)
      ConditionDefaults.item(salamander, Some(own), player, items) shouldBe Start(
        StopCondition.ItemCount(salamander.id, ItemLocation.Inventory, ItemForms.Unnoted, Comparison.AtLeast, 26, stopBefore = false),
        complete = false
      )
    }

    "counts what's already held towards filling the inventory" in {
      ConditionDefaults.item(rope, None, player, items).condition should matchPattern {
        case StopCondition.ItemCount(_, ItemLocation.Inventory, ItemForms.Unnoted, Comparison.AtLeast, 27, false) =>
      }
    }

    "a stackable item suggests the next thousand that's at least 600 more than are held" in {
      def suggestion(held: Int): Int = {
        val stocked = player.copy(depositories =
          Map(Depository.Kind.Inventory -> Depository(Map((feathers.id, false) -> held), Depository.Kind.Inventory))
        )
        ConditionDefaults.item(feathers, None, stocked, items).condition match {
          case StopCondition.ItemCount(_, _, _, Comparison.AtLeast, target, _) => target
          case other => fail(s"Unexpected condition: $other")
        }
      }

      List(0, 270, 400, 430, 600, 1400).map(suggestion) shouldBe List(1000, 1000, 1000, 2000, 2000, 2000)
    }

    "free slots are complete, as the inventory being full" in {
      ConditionDefaults.freeSlots shouldBe
        Start(StopCondition.FreeInventorySlots(Comparison.AtMost, 0, stopBefore = false), complete = true)
    }

    "time is a draft for the step taking a time" in {
      ConditionDefaults.time.complete shouldBe false
      ConditionDefaults.time.condition should matchPattern {
        case StopCondition.TimeElapsed(StopCondition.TimeOrigin.StepStart, _, false) =>
      }
    }
  }
}
