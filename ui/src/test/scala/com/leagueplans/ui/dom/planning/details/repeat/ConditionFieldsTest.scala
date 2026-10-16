package com.leagueplans.ui.dom.planning.details.repeat

import cats.data.NonEmptyList
import com.leagueplans.common.model.{EquipmentType, InfoboxKey, Item, Skill}
import com.leagueplans.ui.model.plan.StopCondition.{Comparison, ItemForms, ItemLocation, TimeOrigin}
import com.leagueplans.ui.model.plan.{Duration, ExpTarget, StopCondition}
import com.leagueplans.ui.model.player.skill.{Exp, Level}
import com.leagueplans.ui.projection.calculation.loops.StopConditions
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class ConditionFieldsTest extends AnyFreeSpec with Matchers {
  "ConditionFields" - {
    val level = new StopCondition.SkillReaches(Skill.Hunter, ExpTarget.AtLevel(Level.L60), stopBefore = false)
    val xp = new StopCondition.SkillReaches(Skill.Hunter, ExpTarget.AtExp(Exp(1000)), stopBefore = false)
    val count = new StopCondition.ItemCount(Item.ID(314), ItemLocation.Inventory, ItemForms.Unnoted, Comparison.AtMost, 0, stopBefore = true)
    val slots = new StopCondition.FreeInventorySlots(Comparison.AtMost, 0, stopBefore = false)
    val time = new StopCondition.TimeElapsed(TimeOrigin.PlanStart, Duration.seconds(5400), stopBefore = false)

    "show each kind's number" in {
      List[StopConditions.Leaf](level, xp, count, slots, time).map(ConditionFields.text) shouldBe List("60", "1000", "0", "0", "1h 30m")
    }

    "read a level from 1 to 99, in the cards' wording" in {
      ConditionFields.parse(level, "70") shouldBe Right(level.copy(target = ExpTarget.AtLevel(Level.L70)))
      ConditionFields.parse(level, "120") shouldBe Left("Type a level from 1 to 99")
    }

    "read xp with thousands separators" in {
      ConditionFields.parse(xp, "13,034,431") shouldBe Right(xp.copy(target = ExpTarget.AtExp(Exp(13034431))))
    }

    "read an amount from 0" in {
      ConditionFields.parse(count, "28") shouldBe Right(count.copy(quantity = 28))
      ConditionFields.parse(count, "-1").isLeft shouldBe true
    }

    "read free slots up to the inventory's size" in {
      ConditionFields.parse(slots, "5") shouldBe Right(slots.copy(slots = 5))
      ConditionFields.parse(slots, "29").isLeft shouldBe true
    }

    "read times past a step's longest duration" in {
      ConditionFields.parse(time, "25h") shouldBe Right(time.copy(atLeast = Duration.seconds(90000)))
      ConditionFields.parse(time, "0").isLeft shouldBe true
    }

    "read a limit, where blank is none" in {
      ConditionFields.parseMax("") shouldBe Right(None)
      ConditionFields.parseMax("300") shouldBe Right(Some(300))
      ConditionFields.parseMax("100,001").isLeft shouldBe true
    }

    def item(id: Int, equipmentType: Option[EquipmentType], noteable: Boolean = true): Item =
      Item(
        Item.ID(id),
        gameID = None,
        name = s"Item $id",
        examine = "",
        NonEmptyList.one((Item.Image.Bin(1), Item.Image.Path("1/1.png"))),
        Item.Bankable.Yes(stacks = true),
        stackable = false,
        noteable,
        equipmentType,
        infobox = InfoboxKey(id, List.empty)
      )

    val feathers = item(314, equipmentType = None)
    val ring = item(1635, Some(EquipmentType.Ring))
    val coins = item(995, equipmentType = None, noteable = false)
    val items: Item.ID => Item = Map(feathers.id -> feathers, ring.id -> ring, coins.id -> coins)

    "only count an item that can be equipped as worn" in {
      ConditionFields.locations(feathers) shouldBe List(ItemLocation.Inventory, ItemLocation.Bank)
      ConditionFields.locations(ring) shouldBe List(ItemLocation.Inventory, ItemLocation.Bank, ItemLocation.Worn)
    }

    "count an item that can't be equipped in the inventory instead of worn" in {
      val worn = new StopCondition.ItemCount(ring.id, ItemLocation.Worn, ItemForms.Unnoted, Comparison.AtLeast, 1, stopBefore = false)
      ConditionFields.fitted(worn, items) shouldBe worn
      ConditionFields.fitted(ConditionFields.asLeaf(worn.copy(item = feathers.id)), items) shouldBe
        worn.copy(item = feathers.id, where = ItemLocation.Inventory)
    }

    "only count noted items in the inventory" in {
      val noted = new StopCondition.ItemCount(feathers.id, ItemLocation.Inventory, ItemForms.Noted, Comparison.AtLeast, 1, stopBefore = false)
      ConditionFields.fitted(noted, items) shouldBe noted
      ConditionFields.fitted(ConditionFields.asLeaf(noted.copy(where = ItemLocation.Bank)), items) shouldBe
        noted.copy(where = ItemLocation.Bank, forms = ItemForms.Unnoted)
    }

    "only count an item that can be noted as noted" in {
      ConditionFields.hasForms(feathers, ItemLocation.Inventory) shouldBe true
      ConditionFields.hasForms(feathers, ItemLocation.Bank) shouldBe false
      ConditionFields.hasForms(coins, ItemLocation.Inventory) shouldBe false

      val noted = new StopCondition.ItemCount(feathers.id, ItemLocation.Inventory, ItemForms.Noted, Comparison.AtLeast, 1, stopBefore = false)
      ConditionFields.fitted(ConditionFields.asLeaf(noted.copy(item = coins.id)), items) shouldBe
        noted.copy(item = coins.id, forms = ItemForms.Unnoted)
    }

    "leave other conditions as they are" in {
      ConditionFields.fitted(level, items) shouldBe level
    }
  }
}
