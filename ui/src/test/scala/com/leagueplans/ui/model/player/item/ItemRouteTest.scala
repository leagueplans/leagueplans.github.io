package com.leagueplans.ui.model.player.item

import cats.data.NonEmptyList
import com.leagueplans.common.model.{EquipmentType, Item}
import com.leagueplans.ui.model.plan.Effect
import com.leagueplans.ui.model.player.item.Depository.Kind
import com.leagueplans.ui.model.player.item.ItemRoute.Place
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class ItemRouteTest extends AnyFreeSpec with Matchers {
  private def item(noteable: Boolean, bankable: Item.Bankable, equipmentType: Option[EquipmentType]): Item =
    Item(
      Item.ID(1),
      gameID = None,
      name = "Test item",
      examine = "",
      NonEmptyList.one((Item.Image.Bin(1), Item.Image.Path("1/1.png"))),
      bankable,
      stackable = false,
      noteable,
      equipmentType
    )

  private val inventory = Place(Kind.Inventory, noted = false)
  private val notes = Place(Kind.Inventory, noted = true)
  private val bank = Place(Kind.Bank, noted = false)
  private val weapon = Place(Kind.EquipmentSlot.Weapon, noted = false)

  "ItemRoute.allFor" - {
    "gives every route for an item that can be banked, noted and worn" in {
      ItemRoute.allFor(item(noteable = true, Item.Bankable.Yes(stacks = true), Some(EquipmentType.Weapon))) shouldBe List(
        ItemRoute(bank, inventory),
        ItemRoute(bank, notes),
        ItemRoute(inventory, bank),
        ItemRoute(notes, bank),
        ItemRoute(inventory, weapon),
        ItemRoute(weapon, inventory),
        ItemRoute(weapon, bank),
        ItemRoute(bank, weapon)
      )
    }

    "never moves notes anywhere but between the bank and the inventory" in {
      val routes = ItemRoute.allFor(item(noteable = true, Item.Bankable.Yes(stacks = true), Some(EquipmentType.Weapon)))
      routes.filter(route => route.from.noted || route.to.noted) shouldBe List(ItemRoute(bank, notes), ItemRoute(notes, bank))
    }

    "leaves out the bank for items that can't be banked" in {
      ItemRoute.allFor(item(noteable = false, Item.Bankable.No, Some(EquipmentType.Weapon))) shouldBe List(
        ItemRoute(inventory, weapon),
        ItemRoute(weapon, inventory)
      )
    }
  }

  "ItemRoute.isPossible rejects a noted item going into an equipment slot" in {
    val scimitar = item(noteable = true, Item.Bankable.Yes(stacks = true), Some(EquipmentType.Weapon))
    val move = new Effect.MoveItem(scimitar.id, 1, Kind.Inventory, notedInSource = true, Kind.EquipmentSlot.Weapon, noteInTarget = false)

    ItemRoute.isPossible(move, scimitar) shouldBe false
    ItemRoute.isPossible(ItemRoute(inventory, weapon).applyTo(move), scimitar) shouldBe true
  }
}
