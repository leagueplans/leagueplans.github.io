package com.leagueplans.ui.model.player.item

import cats.data.NonEmptyList
import com.leagueplans.common.model.{EquipmentType, InfoboxKey, Item}
import com.leagueplans.ui.model.plan.Effect.MoveItem
import com.leagueplans.ui.model.plan.ItemQuantity
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

  private def equip(item: Item, n: Int, from: Kind = Kind.Inventory, slot: EquipmentSlot): MoveItem =
    MoveItem(item.id, ItemQuantity.Exact(n), from, notedInSource = false, slot, noteInTarget = false)

  private def equipAll(item: Item, from: Kind = Kind.Inventory, slot: EquipmentSlot): MoveItem =
    MoveItem(item.id, ItemQuantity.Max, from, notedInSource = false, slot, noteInTarget = false)

  /** Displaced items come off one at a time, or as a whole stack if they stack */
  private def unequip(item: Item, slot: EquipmentSlot): MoveItem =
    MoveItem(item.id, if (item.stackable) ItemQuantity.Max else ItemQuantity.Exact(1), slot, notedInSource = false, Kind.Inventory, noteInTarget = false)

  "EquipPlan" - {
    "equips an item into the slot from its item data" in {
      EquipPlan(helm, Kind.Inventory, player((Kind.Inventory, helm, 1)), items) shouldBe
        Some(List(equip(helm, 1, slot = EquipmentSlot.Head)))
    }

    "equips one copy of an unstackable item" in {
      EquipPlan(sword, Kind.Inventory, player((Kind.Inventory, sword, 3)), items) shouldBe
        Some(List(equip(sword, 1, slot = EquipmentSlot.Weapon)))
    }

    "equips a whole stack of a stackable item" in {
      EquipPlan(arrows, Kind.Bank, player((Kind.Bank, arrows, 250)), items) shouldBe
        Some(List(equipAll(arrows, from = Kind.Bank, slot = EquipmentSlot.Ammo)))
    }

    "equips the given quantity of a stackable item, up to what's held" in {
      EquipPlan(arrows, Kind.Bank, player((Kind.Bank, arrows, 250)), items, ItemQuantity.Exact(10)) shouldBe
        Some(List(equip(arrows, 10, from = Kind.Bank, slot = EquipmentSlot.Ammo)))
      EquipPlan(arrows, Kind.Bank, player((Kind.Bank, arrows, 5)), items, ItemQuantity.Exact(10)) shouldBe
        Some(List(equip(arrows, 5, from = Kind.Bank, slot = EquipmentSlot.Ammo)))
    }

    "moves the slot's current item to the inventory first" in {
      EquipPlan(sword, Kind.Inventory, player((Kind.Inventory, sword, 1), (EquipmentSlot.Weapon, scimitar, 1)), items) shouldBe
        Some(List(unequip(scimitar, EquipmentSlot.Weapon), equip(sword, 1, slot = EquipmentSlot.Weapon)))
    }

    "unequips the shield to equip a two-handed weapon" in {
      EquipPlan(
        greatsword,
        Kind.Inventory,
        player((Kind.Inventory, greatsword, 1), (EquipmentSlot.Weapon, sword, 1), (EquipmentSlot.Shield, shield, 1)),
        items
      ) shouldBe Some(List(
        unequip(sword, EquipmentSlot.Weapon),
        unequip(shield, EquipmentSlot.Shield),
        equip(greatsword, 1, slot = EquipmentSlot.Weapon)
      ))
    }

    "unequips a two-handed weapon to equip a shield, but not a one-handed one" in {
      EquipPlan(shield, Kind.Inventory, player((Kind.Inventory, shield, 1), (EquipmentSlot.Weapon, greatsword, 1)), items) shouldBe
        Some(List(unequip(greatsword, EquipmentSlot.Weapon), equip(shield, 1, slot = EquipmentSlot.Shield)))

      EquipPlan(shield, Kind.Inventory, player((Kind.Inventory, shield, 1), (EquipmentSlot.Weapon, sword, 1)), items) shouldBe
        Some(List(equip(shield, 1, slot = EquipmentSlot.Shield)))
    }

    "adds to an equipped stack of the same stackable item instead of taking it off" in {
      EquipPlan(arrows, Kind.Inventory, player((Kind.Inventory, arrows, 50), (EquipmentSlot.Ammo, arrows, 100)), items) shouldBe
        Some(List(equipAll(arrows, slot = EquipmentSlot.Ammo)))
    }

    "can't equip items without a slot, or that aren't held" in {
      EquipPlan(logs, Kind.Inventory, player((Kind.Inventory, logs, 1)), items) shouldBe None
      EquipPlan(sword, Kind.Inventory, player((Kind.Bank, sword, 1)), items) shouldBe None
    }
  }
}
