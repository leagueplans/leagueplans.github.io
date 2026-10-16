package com.leagueplans.ui.projection.calculation.loops

import cats.data.NonEmptyList
import com.leagueplans.common.model.{InfoboxKey, Item, Skill}
import com.leagueplans.ui.model.plan.{ExpTarget, StopCondition}
import com.leagueplans.ui.model.plan.StopCondition.*
import com.leagueplans.ui.model.player.{GridStatus, Player}
import com.leagueplans.ui.model.player.item.Depository
import com.leagueplans.ui.model.player.item.Depository.Kind
import com.leagueplans.ui.model.player.league.LeagueStatus
import com.leagueplans.ui.model.player.skill.{Exp, Level, Stats}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class StopConditionsTest extends AnyFreeSpec with Matchers {
  "StopConditions" - {
    val feathers = Item.ID(314)
    val logs = Item.ID(1511)

    def item(id: Item.ID, name: String, stackable: Boolean): Item =
      Item(
        id,
        gameID = None,
        name,
        examine = "",
        NonEmptyList.one((Item.Image.Bin(1), Item.Image.Path("1/1.png"))),
        Item.Bankable.Yes(stacks = true),
        stackable = stackable,
        noteable = true,
        equipmentType = None,
        infobox = InfoboxKey(1, List.empty)
      )

    val lookup: Item.ID => Item = Map(
      feathers -> item(feathers, "Feather", stackable = true),
      logs -> item(logs, "Logs", stackable = false)
    )

    val emptyPlayer =
      Player(Stats(), Map.empty, Set.empty, Set.empty, LeagueStatus(0, Set.empty, Set.empty), GridStatus(Set.empty))

    def player(
      exps: Map[Skill, Exp] = Map.empty,
      places: Map[Kind, Map[(Item.ID, Boolean), Int]] = Map.empty
    ): Player =
      emptyPlayer.copy(
        stats = Stats(exps),
        depositories = places.map((kind, contents) => kind -> Depository(contents, kind))
      )

    def ctx(p: Player, shortfalls: Map[(Item.ID, Kind, Boolean), Int] = Map.empty): LoopContext =
      LoopContext(p, lookup, shortfalls)

    def inv(contents: ((Item.ID, Boolean), Int)*): LoopContext =
      ctx(player(places = Map(Kind.Inventory -> contents.toMap)))

    // Tenths of exp
    def exp(skill: Skill, tenths: Int): LoopContext =
      ctx(player(exps = Map(skill -> Exp.tenths(tenths))))

    def two(fishing: Int, hunter: Int): LoopContext =
      ctx(player(exps = Map(Skill.Fishing -> Exp.tenths(fishing), Skill.Hunter -> Exp.tenths(hunter))))

    def expAtLeast(skill: Skill, xp: Int, stopBefore: Boolean = false): SkillReaches =
      SkillReaches(skill, ExpTarget.AtExp(Exp(xp)), stopBefore)

    def count(
      id: Item.ID,
      where: ItemLocation,
      forms: ItemForms,
      comparison: Comparison,
      quantity: Int,
      stopBefore: Boolean = false
    ): ItemCount =
      ItemCount(id, where, forms, comparison, quantity, stopBefore)

    def freeSlots(comparison: Comparison, slots: Int, stopBefore: Boolean = false): FreeInventorySlots =
      FreeInventorySlots(comparison, slots, stopBefore)

    // Exp(100) is 1000 tenths
    val goal100 = expAtLeast(Skill.Fishing, 100)
    val goal100Before = expAtLeast(Skill.Fishing, 100, stopBefore = true)
    val hunter100 = expAtLeast(Skill.Hunter, 100)
    val hunter100Before = expAtLeast(Skill.Hunter, 100, stopBefore = true)

    "remaining" - {
      "skill reaches a level" - {
        val leaf: SkillReaches = SkillReaches(Skill.Hunter, ExpTarget.AtLevel(Level.L60), stopBefore = false)
        val bound = Level.L60.bound.raw

        "is the exp still to gain" in {
          StopConditions.remaining(leaf, exp(Skill.Hunter, 0)) shouldBe (bound.toLong)
          StopConditions.remaining(leaf, exp(Skill.Hunter, 1000)) shouldBe ((bound - 1000).toLong)
        }

        "is zero at exactly the level's bound" in {
          StopConditions.remaining(leaf, exp(Skill.Hunter, bound)) shouldBe 0L
        }

        "is negative past the level" in {
          StopConditions.remaining(leaf, exp(Skill.Hunter, bound + 7)) shouldBe (-7L)
        }

        "ignores other skills" in {
          StopConditions.remaining(leaf, exp(Skill.Fishing, bound)) shouldBe (bound.toLong)
        }
      }

      "skill reaches an amount of exp, in tenths" in {
        StopConditions.remaining(goal100, exp(Skill.Fishing, 0)) shouldBe 1000L
        StopConditions.remaining(goal100, exp(Skill.Fishing, 995)) shouldBe 5L
        StopConditions.remaining(goal100, exp(Skill.Fishing, 1000)) shouldBe 0L
        StopConditions.remaining(goal100, exp(Skill.Fishing, 1003)) shouldBe (-3L)
      }

      "item counts" - {
        val held = inv((feathers, false) -> 10)

        val cases = List(
          (Comparison.AtLeast, 15, 5L),
          (Comparison.AtLeast, 10, 0L),
          (Comparison.AtLeast, 4, -6L),
          (Comparison.AtMost, 15, -5L),
          (Comparison.AtMost, 10, 0L),
          (Comparison.AtMost, 4, 6L)
        )
        cases.foreach { (comparison, quantity, expected) =>
          s"$comparison $quantity of 10 held is $expected" in {
            val leaf = count(feathers, ItemLocation.Inventory, ItemForms.Unnoted, comparison, quantity)
            StopConditions.remaining(leaf, held) shouldBe expected
          }
        }

        "counts none of an item that isn't held" in {
          val leaf = count(logs, ItemLocation.Inventory, ItemForms.Unnoted, Comparison.AtLeast, 3)
          StopConditions.remaining(leaf, held) shouldBe 3L
        }
      }

      "item locations" - {
        // Inventory 1, bank 10, weapon slot 100, ring slot 1000
        val context = ctx(player(places = Map(
          Kind.Inventory -> Map((feathers, false) -> 1),
          Kind.Bank -> Map((feathers, false) -> 10),
          Kind.EquipmentSlot.Weapon -> Map((feathers, false) -> 100),
          Kind.EquipmentSlot.Ring -> Map((feathers, false) -> 1000)
        )))

        // AtMost 0 gives the count itself
        def held(where: ItemLocation): Long =
          StopConditions.remaining(
            count(feathers, where, ItemForms.Unnoted, Comparison.AtMost, 0),
            context
          )

        "Inventory counts only the inventory" in { held(ItemLocation.Inventory) shouldBe 1L }
        "Bank counts only the bank" in { held(ItemLocation.Bank) shouldBe 10L }
        "Worn counts every equipment slot" in { held(ItemLocation.Worn) shouldBe 1100L }
      }

      "item forms" - {
        val context = inv((feathers, false) -> 3, (feathers, true) -> 20)

        def held(forms: ItemForms): Long =
          StopConditions.remaining(
            count(feathers, ItemLocation.Inventory, forms, Comparison.AtMost, 0),
            context
          )

        "Unnoted counts only the unnoted item" in { held(ItemForms.Unnoted) shouldBe 3L }
        "Noted counts only the noted item" in { held(ItemForms.Noted) shouldBe 20L }
      }

      "shortfalls" - {
        val places: Map[Kind, Map[(Item.ID, Boolean), Int]] = Map(
          Kind.Inventory -> Map((feathers, false) -> 5, (feathers, true) -> 7),
          Kind.Bank -> Map((feathers, false) -> 9)
        )

        def held(
          where: ItemLocation,
          forms: ItemForms,
          shortfalls: Map[(Item.ID, Kind, Boolean), Int]
        ): Long =
          StopConditions.remaining(
            count(feathers, where, forms, Comparison.AtMost, 0),
            ctx(player(places = places), shortfalls)
          )

        "subtract from the count" in {
          held(ItemLocation.Inventory, ItemForms.Unnoted, Map((feathers, Kind.Inventory, false) -> 8)) shouldBe (-3L)
        }

        "are ignored for another item" in {
          held(ItemLocation.Inventory, ItemForms.Unnoted, Map((logs, Kind.Inventory, false) -> 8)) shouldBe 5L
        }

        "are ignored for another place" in {
          held(ItemLocation.Inventory, ItemForms.Unnoted, Map((feathers, Kind.Bank, false) -> 8)) shouldBe 5L
        }

        "are ignored for another notedness" in {
          held(ItemLocation.Inventory, ItemForms.Unnoted, Map((feathers, Kind.Inventory, true) -> 8)) shouldBe 5L
        }

        "apply to the noted form" in {
          held(ItemLocation.Inventory, ItemForms.Noted, Map((feathers, Kind.Inventory, true) -> 2)) shouldBe 5L
        }
      }

      "free inventory slots" - {
        def free(comparison: Comparison, slots: Int, context: LoopContext): Long =
          StopConditions.remaining(freeSlots(comparison, slots), context)

        "is 28 for an empty inventory" in {
          free(Comparison.AtLeast, 28, inv()) shouldBe 0L
          free(Comparison.AtMost, 0, inv()) shouldBe 28L
        }

        "gives an unstackable item a slot each" in {
          free(Comparison.AtLeast, 28, inv((logs, false) -> 10)) shouldBe 10L
        }

        "gives a stackable item one slot" in {
          free(Comparison.AtLeast, 28, inv((feathers, false) -> 1000)) shouldBe 1L
        }

        "gives a noted item one slot" in {
          free(Comparison.AtLeast, 28, inv((logs, true) -> 1000)) shouldBe 1L
        }

        "counts a mix of items" in {
          // 5 + 1 + 1 = 7 slots used, 21 free
          val context = inv((logs, false) -> 5, (logs, true) -> 50, (feathers, false) -> 9)
          free(Comparison.AtLeast, 21, context) shouldBe 0L
          free(Comparison.AtLeast, 25, context) shouldBe 4L
          free(Comparison.AtMost, 10, context) shouldBe 11L
          free(Comparison.AtMost, 21, context) shouldBe 0L
          free(Comparison.AtMost, 30, context) shouldBe (-9L)
        }

        "is zero for a full inventory" in {
          val full = inv((logs, false) -> 28)
          free(Comparison.AtMost, 0, full) shouldBe 0L
          free(Comparison.AtLeast, 1, full) shouldBe 1L
        }
      }
    }

    "met" - {
      "is true for a leaf at exactly zero remaining" in {
        StopConditions.met(goal100, exp(Skill.Fishing, 1000)) shouldBe true
      }

      "is true for a leaf with negative remaining" in {
        StopConditions.met(goal100, exp(Skill.Fishing, 1001)) shouldBe true
      }

      "is false for a leaf with positive remaining" in {
        StopConditions.met(goal100, exp(Skill.Fishing, 999)) shouldBe false
      }

      "does not care whether a leaf is stopBefore" in {
        StopConditions.met(goal100Before, exp(Skill.Fishing, 999)) shouldBe false
        StopConditions.met(goal100Before, exp(Skill.Fishing, 1000)) shouldBe true
      }

      "And needs both sides" in {
        val both = And(goal100, hunter100)
        StopConditions.met(both, two(1000, 1000)) shouldBe true
        StopConditions.met(both, two(999, 1000)) shouldBe false
        StopConditions.met(both, two(1000, 999)) shouldBe false
        StopConditions.met(both, two(0, 0)) shouldBe false
      }

      "Or needs either side" in {
        val either = Or(goal100, hunter100)
        StopConditions.met(either, two(1000, 0)) shouldBe true
        StopConditions.met(either, two(0, 1000)) shouldBe true
        StopConditions.met(either, two(1000, 1000)) shouldBe true
        StopConditions.met(either, two(999, 999)) shouldBe false
      }
    }

    "metBefore" - {
      "counts a stopBefore leaf as met when the next context passes it" in {
        StopConditions.metBefore(goal100Before, exp(Skill.Fishing, 500), exp(Skill.Fishing, 1001)) shouldBe true
      }

      "does not count a stopBefore leaf as met when the next context merely reaches it" in {
        StopConditions.metBefore(goal100Before, exp(Skill.Fishing, 500), exp(Skill.Fishing, 1000)) shouldBe false
      }

      "does not count a stopBefore leaf as met when the next context is short of it" in {
        StopConditions.metBefore(goal100Before, exp(Skill.Fishing, 500), exp(Skill.Fishing, 600)) shouldBe false
      }

      "counts a stopBefore leaf that is already met now as met" in {
        StopConditions.metBefore(goal100Before, exp(Skill.Fishing, 1000), exp(Skill.Fishing, 1000)) shouldBe true
      }

      "ignores the next context for a leaf that isn't stopBefore" in {
        StopConditions.metBefore(goal100, exp(Skill.Fishing, 500), exp(Skill.Fishing, 1001)) shouldBe false
        StopConditions.metBefore(goal100, exp(Skill.Fishing, 1000), exp(Skill.Fishing, 0)) shouldBe true
      }

      "works for item counts that the next repetition would take below the threshold" in {
        val leaf = count(feathers, ItemLocation.Inventory, ItemForms.Unnoted, Comparison.AtMost, 0, stopBefore = true)
        val now = inv((feathers, false) -> 3)
        // The repetition tried to take 2 more feathers than there were
        val next = ctx(player(), Map((feathers, Kind.Inventory, false) -> 2))
        StopConditions.metBefore(leaf, now, next) shouldBe true
        StopConditions.metBefore(leaf, now, inv()) shouldBe false
      }

      "inside And needs both sides" in {
        val both = And(goal100Before, hunter100Before)
        StopConditions.metBefore(both, two(500, 500), two(1001, 600)) shouldBe false
        StopConditions.metBefore(both, two(500, 500), two(1001, 1001)) shouldBe true
      }

      "inside And mixes met and stopBefore leaves" in {
        val both = And(goal100, hunter100Before)
        StopConditions.metBefore(both, two(1000, 500), two(1000, 1001)) shouldBe true
        StopConditions.metBefore(both, two(500, 500), two(500, 1001)) shouldBe false
      }

      "inside Or needs only one side" in {
        val either = Or(goal100Before, hunter100Before)
        StopConditions.metBefore(either, two(500, 500), two(1001, 600)) shouldBe true
        StopConditions.metBefore(either, two(500, 500), two(1000, 1000)) shouldBe false
      }

      "inside Or ignores the next context for leaves that aren't stopBefore" in {
        val either = Or(goal100, hunter100)
        StopConditions.metBefore(either, two(500, 500), two(1001, 1001)) shouldBe false
      }
    }

    "passed" - {
      "includes a stopBefore leaf that goes from >= 0 to < 0" in {
        StopConditions.passed(goal100Before, exp(Skill.Fishing, 500), exp(Skill.Fishing, 1001)) shouldBe List(goal100Before)
        StopConditions.passed(goal100Before, exp(Skill.Fishing, 1000), exp(Skill.Fishing, 1001)) shouldBe List(goal100Before)
      }

      "excludes a stopBefore leaf that only reaches its threshold" in {
        StopConditions.passed(goal100Before, exp(Skill.Fishing, 500), exp(Skill.Fishing, 1000)) shouldBe empty
      }

      "excludes a stopBefore leaf that was already past its threshold" in {
        StopConditions.passed(goal100Before, exp(Skill.Fishing, 1001), exp(Skill.Fishing, 1002)) shouldBe empty
      }

      "excludes a leaf that isn't stopBefore" in {
        StopConditions.passed(goal100, exp(Skill.Fishing, 500), exp(Skill.Fishing, 1001)) shouldBe empty
      }

      "finds leaves inside And and Or, and only the stopBefore ones" in {
        val condition = And(goal100Before, Or(hunter100, expAtLeast(Skill.Mining, 1, stopBefore = true)))
        val before = ctx(player(exps = Map(
          Skill.Fishing -> Exp.tenths(500), Skill.Hunter -> Exp.tenths(500), Skill.Mining -> Exp.tenths(5)
        )))
        val after = ctx(player(exps = Map(
          Skill.Fishing -> Exp.tenths(1001), Skill.Hunter -> Exp.tenths(1001), Skill.Mining -> Exp.tenths(11)
        )))
        StopConditions.passed(condition, before, after) shouldBe
          List(goal100Before, expAtLeast(Skill.Mining, 1, stopBefore = true))
      }
    }

    "progressed" - {
      "is true for a leaf that got closer" in {
        StopConditions.progressed(goal100, exp(Skill.Fishing, 0), exp(Skill.Fishing, 10)) shouldBe true
      }

      "is false for a leaf that stayed put" in {
        StopConditions.progressed(goal100, exp(Skill.Fishing, 10), exp(Skill.Fishing, 10)) shouldBe false
      }

      "is false for a leaf that got further" in {
        StopConditions.progressed(goal100, exp(Skill.Fishing, 10), exp(Skill.Fishing, 0)) shouldBe false
      }

      "works for AtMost item counts" in {
        val leaf = count(feathers, ItemLocation.Inventory, ItemForms.Unnoted, Comparison.AtMost, 0)
        StopConditions.progressed(leaf, inv((feathers, false) -> 5), inv((feathers, false) -> 3)) shouldBe true
        StopConditions.progressed(leaf, inv((feathers, false) -> 3), inv((feathers, false) -> 5)) shouldBe false
      }

      "And needs every unmet child to progress" in {
        val both = And(goal100, hunter100)
        StopConditions.progressed(both, two(0, 0), two(10, 10)) shouldBe true
        StopConditions.progressed(both, two(0, 0), two(10, 0)) shouldBe false
        StopConditions.progressed(both, two(0, 0), two(0, 10)) shouldBe false
      }

      "And treats a child that became met as fine" in {
        val both = And(goal100, hunter100)
        StopConditions.progressed(both, two(0, 0), two(10, 1000)) shouldBe true
        StopConditions.progressed(both, two(0, 0), two(1000, 10)) shouldBe true
      }

      "Or needs any child to progress" in {
        val either = Or(goal100, hunter100)
        StopConditions.progressed(either, two(0, 0), two(10, 0)) shouldBe true
        StopConditions.progressed(either, two(0, 0), two(0, 10)) shouldBe true
        StopConditions.progressed(either, two(0, 0), two(0, 0)) shouldBe false
      }
    }

    "estimate" - {
      "of a leaf is the remaining over the progress, rounded up" in {
        // 700 left after 300 of progress, so ceil(700 / 300) = 3
        StopConditions.estimate(goal100, exp(Skill.Fishing, 0), exp(Skill.Fishing, 300)) shouldBe Some(3L)
      }

      "of a leaf is exact when the progress divides the remaining" in {
        StopConditions.estimate(goal100, exp(Skill.Fishing, 0), exp(Skill.Fishing, 250)) shouldBe Some(3L)
        StopConditions.estimate(goal100, exp(Skill.Fishing, 0), exp(Skill.Fishing, 500)) shouldBe Some(1L)
      }

      "of a leaf is zero once met" in {
        StopConditions.estimate(goal100, exp(Skill.Fishing, 900), exp(Skill.Fishing, 1000)) shouldBe Some(0L)
        StopConditions.estimate(goal100, exp(Skill.Fishing, 900), exp(Skill.Fishing, 1100)) shouldBe Some(0L)
      }

      "of a leaf is zero once met, even without progress" in {
        StopConditions.estimate(goal100, exp(Skill.Fishing, 1000), exp(Skill.Fishing, 1000)) shouldBe Some(0L)
      }

      "of an unmet leaf is None without progress" in {
        StopConditions.estimate(goal100, exp(Skill.Fishing, 10), exp(Skill.Fishing, 10)) shouldBe None
        StopConditions.estimate(goal100, exp(Skill.Fishing, 10), exp(Skill.Fishing, 0)) shouldBe None
      }

      "of And is the largest estimate of the unmet children" in {
        // Fishing: 900 left at 100 a go is 9. Hunter: 500 left at 500 a go is 1.
        StopConditions.estimate(And(goal100, hunter100), two(0, 0), two(100, 500)) shouldBe Some(9L)
      }

      "of And ignores children that are met" in {
        StopConditions.estimate(And(goal100, hunter100), two(0, 0), two(100, 1000)) shouldBe Some(9L)
      }

      "of And is zero when both are met" in {
        StopConditions.estimate(And(goal100, hunter100), two(0, 0), two(1000, 1000)) shouldBe Some(0L)
      }

      "of And is None if any unmet child has None" in {
        StopConditions.estimate(And(goal100, hunter100), two(0, 0), two(100, 0)) shouldBe None
      }

      "of Or is the smallest of the estimates there are" in {
        StopConditions.estimate(Or(goal100, hunter100), two(0, 0), two(100, 500)) shouldBe Some(1L)
      }

      "of Or ignores children with no estimate" in {
        StopConditions.estimate(Or(goal100, hunter100), two(0, 0), two(100, 0)) shouldBe Some(9L)
      }

      "of Or is None if no child has an estimate" in {
        StopConditions.estimate(Or(goal100, hunter100), two(0, 0), two(0, 0)) shouldBe None
      }

      "of Or is zero when either is met" in {
        StopConditions.estimate(Or(goal100, hunter100), two(0, 0), two(1000, 0)) shouldBe Some(0L)
      }
    }

    "describe" - {
      def describe(condition: StopCondition): String =
        StopConditions.describe(condition, lookup)

      val a = expAtLeast(Skill.Fishing, 1)
      val b = expAtLeast(Skill.Hunter, 2)
      val c = expAtLeast(Skill.Mining, 3)

      "a skill reaching a level" in {
        describe(SkillReaches(Skill.Hunter, ExpTarget.AtLevel(Level.L60), stopBefore = false)) shouldBe
          "Hunter reaches level 60"
      }

      "a skill reaching an amount of exp" in {
        describe(expAtLeast(Skill.Fishing, 1500000)) shouldBe "Fishing reaches 1,500,000 xp"
      }

      "an item count in each place" in {
        def at(where: ItemLocation) = describe(count(logs, where, ItemForms.Unnoted, Comparison.AtLeast, 5))
        at(ItemLocation.Inventory) shouldBe "the inventory holds at least 5 Logs"
        at(ItemLocation.Bank) shouldBe "the bank holds at least 5 Logs"
        at(ItemLocation.Worn) shouldBe "at least 5 Logs are worn"
      }

      "an item count with the other comparison and thousands separators" in {
        describe(count(logs, ItemLocation.Bank, ItemForms.Unnoted, Comparison.AtMost, 12345)) shouldBe
          "the bank holds at most 12,345 Logs"
      }

      "an item count with each form" in {
        def forms(f: ItemForms) = describe(count(logs, ItemLocation.Inventory, f, Comparison.AtLeast, 1))
        forms(ItemForms.Unnoted) shouldBe "the inventory holds at least 1 Logs"
        forms(ItemForms.Noted) shouldBe "the inventory holds at least 1 Logs (noted)"
      }

      "free inventory slots" in {
        describe(freeSlots(Comparison.AtLeast, 5)) shouldBe "the inventory has at least 5 free slots"
        describe(freeSlots(Comparison.AtMost, 3)) shouldBe "the inventory has at most 3 free slots"
      }

      "a single free inventory slot is singular" in {
        describe(freeSlots(Comparison.AtLeast, 1)) shouldBe "the inventory has at least 1 free slot"
      }

      "at most no free inventory slots is a full inventory" in {
        describe(freeSlots(Comparison.AtMost, 0)) shouldBe "the inventory is full"
      }

      "a repetition going past a condition" in {
        StopConditions.describePassing(new SkillReaches(Skill.Hunter, ExpTarget.AtLevel(Level.L60), stopBefore = true), lookup) shouldBe
          "Hunter past level 60"
        StopConditions.describePassing(new ItemCount(logs, ItemLocation.Bank, ItemForms.Noted, Comparison.AtMost, 0, stopBefore = true), lookup) shouldBe
          "Logs (noted) in the bank below 0"
        StopConditions.describePassing(new FreeInventorySlots(Comparison.AtLeast, 4, stopBefore = true), lookup) shouldBe
          "the inventory's free slots above 4"
      }

      "time passing" in {
        describe(TimeElapsed(TimeOrigin.StepStart, com.leagueplans.ui.model.plan.Duration.seconds(5400), stopBefore = false)) shouldBe
          "this step has taken 1h 30m"
        describe(TimeElapsed(TimeOrigin.PlanStart, com.leagueplans.ui.model.plan.Duration.seconds(90061), stopBefore = false)) shouldBe
          "the plan reaches 25h 1m 1s"
        describe(TimeElapsed(TimeOrigin.StepStart, com.leagueplans.ui.model.plan.Duration.ticks(1), stopBefore = false)) shouldBe
          "this step has taken 1 tick"
      }

      "at least no free inventory slots is not special" in {
        describe(freeSlots(Comparison.AtLeast, 0)) shouldBe "the inventory has at least 0 free slots"
      }

      "And and Or join their sides" in {
        val hunter60 = SkillReaches(Skill.Hunter, ExpTarget.AtLevel(Level.L60), stopBefore = false)
        val full = freeSlots(Comparison.AtMost, 0)
        describe(Or(hunter60, full)) shouldBe "Hunter reaches level 60 or the inventory is full"
        describe(And(hunter60, full)) shouldBe "Hunter reaches level 60 and the inventory is full"
      }

      "chains of the same operator need no parentheses" in {
        describe(And(And(a, b), c)) shouldBe "Fishing reaches 1 xp and Hunter reaches 2 xp and Mining reaches 3 xp"
        describe(Or(a, Or(b, c))) shouldBe "Fishing reaches 1 xp or Hunter reaches 2 xp or Mining reaches 3 xp"
      }

      "an Or inside an And is parenthesised" in {
        describe(And(Or(a, b), c)) shouldBe "(Fishing reaches 1 xp or Hunter reaches 2 xp) and Mining reaches 3 xp"
        describe(And(a, Or(b, c))) shouldBe "Fishing reaches 1 xp and (Hunter reaches 2 xp or Mining reaches 3 xp)"
      }

      "an And inside an Or is parenthesised" in {
        describe(Or(And(a, b), c)) shouldBe "(Fishing reaches 1 xp and Hunter reaches 2 xp) or Mining reaches 3 xp"
        describe(Or(a, And(b, c))) shouldBe "Fishing reaches 1 xp or (Hunter reaches 2 xp and Mining reaches 3 xp)"
      }

      "a condition already met" in {
        StopConditions.describeAlreadyMet(SkillReaches(Skill.Agility, ExpTarget.AtLevel(Level.L40), stopBefore = false), lookup) shouldBe
          "Agility is already level 40"
        StopConditions.describeAlreadyMet(count(logs, ItemLocation.Bank, ItemForms.Unnoted, Comparison.AtLeast, 5), lookup) shouldBe
          "the bank already holds at least 5 Logs"
        StopConditions.describeAlreadyMet(freeSlots(Comparison.AtMost, 0), lookup) shouldBe "the inventory is already full"
      }

      "a condition a repetition should bring closer" in {
        StopConditions.describeApproach(SkillReaches(Skill.Firemaking, ExpTarget.AtLevel(Level.L50), stopBefore = false), lookup) shouldBe
          "Firemaking closer to level 50"
        StopConditions.describeApproach(count(feathers, ItemLocation.Inventory, ItemForms.Unnoted, Comparison.AtMost, 0), lookup) shouldBe
          "Feather in the inventory closer to 0"
      }

      "a condition a step stops before passing" in {
        StopConditions.describeStoppingBefore(count(feathers, ItemLocation.Inventory, ItemForms.Unnoted, Comparison.AtMost, 0), lookup) shouldBe
          "Feather runs out"
        StopConditions.describeStoppingBefore(count(logs, ItemLocation.Bank, ItemForms.Noted, Comparison.AtLeast, 10), lookup) shouldBe
          "Logs (noted) in the bank goes above 10"
        StopConditions.describeStoppingBefore(
          TimeElapsed(TimeOrigin.PlanStart, com.leagueplans.ui.model.plan.Duration.seconds(90000), stopBefore = true),
          lookup
        ) shouldBe "the plan passes 25h"
      }
    }

    "metLeaves" - {
      "lists the conditions that are met" in {
        StopConditions.metLeaves(Or(goal100, hunter100), two(fishing = 1000, hunter = 0)) shouldBe List(goal100)
        StopConditions.metLeaves(And(goal100, hunter100), two(fishing = 1000, hunter = 1000)) shouldBe List(goal100, hunter100)
      }
    }

    "sufficientLeaves" - {
      "is a lone condition" in {
        StopConditions.sufficientLeaves(goal100) shouldBe List(goal100)
      }

      "is every condition combined with or" in {
        StopConditions.sufficientLeaves(Or(goal100, hunter100)) shouldBe List(goal100, hunter100)
      }

      "leaves out conditions combined with and" in {
        StopConditions.sufficientLeaves(Or(And(goal100, hunter100), goal100Before)) shouldBe List(goal100Before)
      }
    }
  }
}
