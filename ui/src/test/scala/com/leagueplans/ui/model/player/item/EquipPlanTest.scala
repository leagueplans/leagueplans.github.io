package com.leagueplans.ui.model.player.item

import cats.data.NonEmptyList
import com.leagueplans.common.model.{EquipmentType, InfoboxKey, Item}
import com.leagueplans.ui.model.plan.Effect.MoveItem
import com.leagueplans.ui.model.plan.ItemQuantity
import com.leagueplans.ui.model.plan.ItemQuantity.{Exact, Max}
import com.leagueplans.ui.model.player.item.Depository.Kind
import com.leagueplans.ui.model.player.item.Depository.Kind.EquipmentSlot
import com.leagueplans.ui.model.player.league.LeagueStatus
import com.leagueplans.ui.model.player.skill.Stats
import com.leagueplans.ui.model.player.{GridStatus, Player}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class EquipPlanTest extends AnyFreeSpec with Matchers {
  private def item(id: Int, equipmentType: Option[EquipmentType], stackable: Boolean = false): Item =
    Item(
      Item.ID(id),
      gameID = None,
      name = s"Item $id",
      examine = "",
      NonEmptyList.one((Item.Image.Bin(1), Item.Image.Path("1/1.png"))),
      Item.Bankable.Yes(stacks = true),
      stackable,
      noteable = true,
      equipmentType,
      infobox = InfoboxKey(1, List.empty)
    )

  private val sword = item(1, Some(EquipmentType.Weapon))
  private val scimitar = item(2, Some(EquipmentType.Weapon))
  private val shield = item(3, Some(EquipmentType.Shield))
  private val greatsword = item(4, Some(EquipmentType.TwoHanded))
  private val arrows = item(5, Some(EquipmentType.Ammo), stackable = true)
  private val helm = item(6, Some(EquipmentType.Head))
  private val logs = item(7, None)
  private val items = List(sword, scimitar, shield, greatsword, arrows, helm, logs).map(i => i.id -> i).toMap

  private def player(contents: (Kind, Item, Int)*): Player =
    Player(
      Stats(),
      contents
        .groupBy((kind, _, _) => kind)
        .map((kind, entries) =>
          kind -> Depository(entries.map((_, item, n) => (item.id, false) -> n).toMap, kind)
        ),
      Set.empty,
      Set.empty,
      LeagueStatus(0, Set.empty, Set.empty),
      GridStatus(Set.empty)
    )

  private def wear(item: Item, n: ItemQuantity, from: Kind = Kind.Inventory, slot: EquipmentSlot): MoveItem =
    MoveItem(item.id, n, from, notedInSource = false, slot, noteInTarget = false)

  private def takeOff(item: Item, n: ItemQuantity, slot: EquipmentSlot): MoveItem =
    MoveItem(item.id, n, slot, notedInSource = false, Kind.Inventory, noteInTarget = false)

  "EquipPlan" - {
    "wears an item into the slot from its item data" in {
      EquipPlan(helm, Kind.Inventory, player((Kind.Inventory, helm, 1)), items) shouldBe
        Some(List(wear(helm, Exact(1), slot = EquipmentSlot.Head)))
    }

    "wears one copy of an unstackable item" in {
      EquipPlan(sword, Kind.Inventory, player((Kind.Inventory, sword, 3)), items) shouldBe
        Some(List(wear(sword, Exact(1), slot = EquipmentSlot.Weapon)))
    }

    "wears a whole stack of a stackable item" in {
      EquipPlan(arrows, Kind.Bank, player((Kind.Bank, arrows, 250)), items) shouldBe
        Some(List(wear(arrows, Max, from = Kind.Bank, slot = EquipmentSlot.Ammo)))
    }

    "moves the slot's current item to the inventory first" in {
      EquipPlan(sword, Kind.Inventory, player((Kind.Inventory, sword, 1), (EquipmentSlot.Weapon, scimitar, 1)), items) shouldBe
        Some(List(takeOff(scimitar, Exact(1), EquipmentSlot.Weapon), wear(sword, Exact(1), slot = EquipmentSlot.Weapon)))
    }

    "takes off the shield to wield a two-handed weapon" in {
      EquipPlan(
        greatsword,
        Kind.Inventory,
        player((Kind.Inventory, greatsword, 1), (EquipmentSlot.Weapon, sword, 1), (EquipmentSlot.Shield, shield, 1)),
        items
      ) shouldBe Some(List(
        takeOff(sword, Exact(1), EquipmentSlot.Weapon),
        takeOff(shield, Exact(1), EquipmentSlot.Shield),
        wear(greatsword, Exact(1), slot = EquipmentSlot.Weapon)
      ))
    }

    "takes off a two-handed weapon to wear a shield, but not a one-handed one" in {
      EquipPlan(shield, Kind.Inventory, player((Kind.Inventory, shield, 1), (EquipmentSlot.Weapon, greatsword, 1)), items) shouldBe
        Some(List(takeOff(greatsword, Exact(1), EquipmentSlot.Weapon), wear(shield, Exact(1), slot = EquipmentSlot.Shield)))

      EquipPlan(shield, Kind.Inventory, player((Kind.Inventory, shield, 1), (EquipmentSlot.Weapon, sword, 1)), items) shouldBe
        Some(List(wear(shield, Exact(1), slot = EquipmentSlot.Shield)))
    }

    "adds to a worn stack of the same stackable item instead of taking it off" in {
      EquipPlan(arrows, Kind.Inventory, player((Kind.Inventory, arrows, 50), (EquipmentSlot.Ammo, arrows, 100)), items) shouldBe
        Some(List(wear(arrows, Max, slot = EquipmentSlot.Ammo)))
    }

    "can't wear items without a slot, or that aren't held" in {
      EquipPlan(logs, Kind.Inventory, player((Kind.Inventory, logs, 1)), items) shouldBe None
      EquipPlan(sword, Kind.Inventory, player((Kind.Bank, sword, 1)), items) shouldBe None
    }
  }
}
