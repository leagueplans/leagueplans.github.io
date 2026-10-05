package com.leagueplans.ui.model.player.item

import cats.data.NonEmptyList
import com.leagueplans.common.model.{EquipmentType, InfoboxKey, Item}
import com.leagueplans.ui.model.plan.Effect.{AddItem, MoveItem}
import com.leagueplans.ui.model.plan.ItemChange
import com.leagueplans.ui.model.plan.ItemQuantity.Exact
import com.leagueplans.ui.model.player.item.Depository.Kind
import com.leagueplans.ui.model.player.item.Depository.Kind.EquipmentSlot
import com.leagueplans.ui.model.player.item.ItemActions.Holding
import com.leagueplans.ui.model.player.league.LeagueStatus
import com.leagueplans.ui.model.player.skill.Stats
import com.leagueplans.ui.model.player.{GridStatus, Player}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class ItemActionsTest extends AnyFreeSpec with Matchers {
  private def item(
    id: Int,
    name: String,
    equipmentType: Option[EquipmentType] = None,
    noteable: Boolean = true,
    bankable: Item.Bankable = Item.Bankable.Yes(stacks = true)
  ): Item =
    Item(
      Item.ID(id),
      gameID = None,
      name,
      examine = "",
      NonEmptyList.one((Item.Image.Bin(1), Item.Image.Path("1/1.png"))),
      bankable,
      stackable = false,
      noteable,
      equipmentType,
      infobox = InfoboxKey(1, List.empty)
    )

  private val logs = item(1, "Logs")
  private val scimitar = item(2, "Rune scimitar", Some(EquipmentType.Weapon))
  private val helm = item(3, "Rune full helm", Some(EquipmentType.Head))
  private val sword = item(4, "Bronze sword", Some(EquipmentType.Weapon))
  private val book = item(5, "Quest book", noteable = false, bankable = Item.Bankable.No)
  private val items = List(logs, scimitar, helm, sword, book).map(i => i.id -> i).toMap

  private val player =
    Player(
      Stats(),
      Map(
        Kind.Inventory -> Depository(Map((logs.id, false) -> 3, (logs.id, true) -> 20, (scimitar.id, false) -> 1), Kind.Inventory),
        Kind.Bank -> Depository(Map((logs.id, false) -> 100), Kind.Bank),
        EquipmentSlot.Weapon -> Depository(Map((sword.id, false) -> 1), EquipmentSlot.Weapon)
      ),
      Set.empty,
      Set.empty,
      LeagueStatus(0, Set.empty, Set.empty),
      GridStatus(Set.empty)
    )

  "ItemActions" - {
    "counts the stack held where it is, noted and unnoted apart" in {
      ItemActions.held(Holding(logs, noted = false, Kind.Inventory), player) shouldBe 3
      ItemActions.held(Holding(logs, noted = true, Kind.Inventory), player) shouldBe 20
      ItemActions.held(Holding(logs, noted = false, Kind.Bank), player) shouldBe 100
    }

    "banks noted items as unnoted" in {
      ItemActions.bank(Holding(logs, noted = true, Kind.Inventory), 20).effects shouldBe
        List(MoveItem(logs.id, Exact(20), Kind.Inventory, notedInSource = true, Kind.Bank, noteInTarget = false))
    }

    "withdraws from the bank, noted if asked" in {
      val action = ItemActions.withdraw(Holding(logs, noted = false, Kind.Bank), 25, noted = true)
      action.effects shouldBe List(MoveItem(logs.id, Exact(25), Kind.Bank, notedInSource = false, Kind.Inventory, noteInTarget = true))
      action.report shouldBe "Withdrew 25 × Logs as notes"
    }

    "removes from where the stack is held" in {
      ItemActions.remove(Holding(logs, noted = true, Kind.Inventory), 5).effects shouldBe
        List(AddItem(logs.id, ItemChange.By(-5), Kind.Inventory, true))
    }

    "adds more copies to the bank for a banked stack, and to the inventory otherwise" in {
      ItemActions.addMore(Holding(logs, noted = false, Kind.Bank), 10).effects shouldBe
        List(AddItem(logs.id, ItemChange.By(10), Kind.Bank, false))
      ItemActions.addMore(Holding(logs, noted = true, Kind.Inventory), 10).effects shouldBe
        List(AddItem(logs.id, ItemChange.By(10), Kind.Inventory, true))
      ItemActions.addMore(Holding(helm, noted = false, EquipmentSlot.Head), 1).effects shouldBe
        List(AddItem(helm.id, ItemChange.By(1), Kind.Inventory, false))
    }

    "never adds notes of an item that can't be noted" in {
      ItemActions.add(book, 1, Kind.Inventory, noted = true).effects shouldBe
        List(AddItem(book.id, ItemChange.By(1), Kind.Inventory, false))
    }

    "wields a weapon, saying what it took off" in {
      val action = ItemActions.wear(Holding(scimitar, noted = false, Kind.Inventory), player, items)
      action.map(_.report) shouldBe Some("Wielded Rune scimitar")
      action.flatMap(_.detail) shouldBe Some("Took off Bronze sword")
      action.map(_.effects.size) shouldBe Some(2)
    }

    "only offers what suits the stack" in {
      ItemActions.canBank(Holding(book, noted = false, Kind.Inventory)) shouldBe false
      ItemActions.canBank(Holding(logs, noted = false, Kind.Bank)) shouldBe false
      ItemActions.canWear(Holding(scimitar, noted = true, Kind.Inventory)) shouldBe false
      ItemActions.canWear(Holding(scimitar, noted = false, Kind.Bank)) shouldBe false
      ItemActions.canWithdrawNoted(Holding(book, noted = false, Kind.Bank)) shouldBe false
      ItemActions.wearLabel(helm) shouldBe "Wear"
    }

    "describes quantities with thousands separators" in {
      ItemActions.describe(logs, 1) shouldBe "Logs"
      ItemActions.describe(logs, 12500) shouldBe "12,500 × Logs"
    }
  }
}
