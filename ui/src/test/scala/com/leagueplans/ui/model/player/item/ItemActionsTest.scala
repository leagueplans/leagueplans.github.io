package com.leagueplans.ui.model.player.item

import cats.data.NonEmptyList
import com.leagueplans.common.model.{EquipmentType, InfoboxKey, Item}
import com.leagueplans.ui.model.plan.Effect.{AddItem, DepositAll, DepositSource, MoveItem}
import com.leagueplans.ui.model.plan.{ItemChange, ItemQuantity}
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

  private def buttons(holding: Holding): List[String] =
    ItemActions.cardButtons(holding, items).map {
      case ItemActions.CardButton.Whole(label, _) => label
      case button: ItemActions.CardButton.WithAmount => s"${button.label} (amount)"
    }

  "ItemActions" - {
    "offers buttons on a card for what can be done where the stack is held" in {
      buttons(Holding(scimitar, noted = false, Kind.Inventory)) shouldBe
        List("Equip", "Bank (amount)", "Remove (amount)", "Add (amount)")
      buttons(Holding(book, noted = false, Kind.Inventory)) shouldBe List("Remove (amount)", "Add (amount)")
      buttons(Holding(scimitar, noted = false, Kind.Bank)) shouldBe
        List("Equip", "Withdraw (amount)", "Withdraw noted (amount)")
      buttons(Holding(sword, noted = false, EquipmentSlot.Weapon)) shouldBe List("Unequip", "Bank")
      buttons(Holding(book, noted = false, EquipmentSlot.Weapon)) shouldBe List("Unequip")
    }

    "only adds until full for items that take a slot each" in {
      val add = ItemActions.cardButtons(Holding(logs, noted = true, Kind.Inventory), items).collectFirst {
        case button: ItemActions.CardButton.WithAmount if button.label == "Add" => button
      }
      add.flatMap(_.unavailable(ItemQuantity.Max)) shouldBe Some(ItemActions.fillReason)
      add.flatMap(_.unavailable(Exact(5))) shouldBe None
    }

    "counts the stack held where it is, noted and unnoted apart" in {
      ItemActions.held(Holding(logs, noted = false, Kind.Inventory), player) shouldBe 3
      ItemActions.held(Holding(logs, noted = true, Kind.Inventory), player) shouldBe 20
      ItemActions.held(Holding(logs, noted = false, Kind.Bank), player) shouldBe 100
    }

    "banks noted items as unnoted" in {
      ItemActions.bank(Holding(logs, noted = true, Kind.Inventory), Exact(20)).effects shouldBe
        List(MoveItem(logs.id, Exact(20), Kind.Inventory, notedInSource = true, Kind.Bank, noteInTarget = false))
    }

    "withdraws from the bank, noted if asked" in {
      val action = ItemActions.withdraw(Holding(logs, noted = false, Kind.Bank), Exact(25), noted = true)
      action.effects shouldBe List(MoveItem(logs.id, Exact(25), Kind.Bank, notedInSource = false, Kind.Inventory, noteInTarget = true))
      action.report shouldBe "Withdrew 25 × Logs as notes"
    }

    "withdraws all, worked out where the effect applies" in {
      val action = ItemActions.withdraw(Holding(logs, noted = false, Kind.Bank), ItemQuantity.Max, noted = false)
      action.effects shouldBe List(MoveItem(logs.id, ItemQuantity.Max, Kind.Bank, notedInSource = false, Kind.Inventory, noteInTarget = false))
      action.report shouldBe "Withdrew all Logs"
    }

    "removes the most of a stack by emptying it" in {
      ItemActions.remove(Holding(logs, noted = false, Kind.Bank), ItemQuantity.Max).effects shouldBe
        List(AddItem(logs.id, ItemChange.Empty, Kind.Bank, false))
    }

    "removes from where the stack is held" in {
      ItemActions.remove(Holding(logs, noted = true, Kind.Inventory), Exact(5)).effects shouldBe
        List(AddItem(logs.id, ItemChange.By(-5), Kind.Inventory, true))
    }

    "always adds more copies to the inventory" in {
      ItemActions.addMore(Holding(logs, noted = false, Kind.Bank), Exact(10)).effects shouldBe
        List(AddItem(logs.id, ItemChange.By(10), Kind.Inventory, false))
      ItemActions.addMore(Holding(logs, noted = true, Kind.Inventory), Exact(10)).effects shouldBe
        List(AddItem(logs.id, ItemChange.By(10), Kind.Inventory, true))
      ItemActions.addMore(Holding(helm, noted = false, EquipmentSlot.Head), Exact(1)).effects shouldBe
        List(AddItem(helm.id, ItemChange.By(1), Kind.Inventory, false))
    }

    "fills only where each item takes a slot of its own" in {
      ItemEffects.canFill(logs, noted = false, Kind.Inventory) shouldBe true
      ItemEffects.canFill(logs, noted = true, Kind.Inventory) shouldBe false
      ItemEffects.canFill(logs, noted = false, Kind.Bank) shouldBe false
      val fill = ItemActions.add(logs, ItemQuantity.Max, Kind.Inventory, noted = false)
      fill.effects shouldBe List(AddItem(logs.id, ItemChange.Fill, Kind.Inventory, false))
      fill.report shouldBe "Added Logs until the inventory was full"
    }

    "never adds notes of an item that can't be noted" in {
      ItemActions.add(book, Exact(1), Kind.Inventory, noted = true).effects shouldBe
        List(AddItem(book.id, ItemChange.By(1), Kind.Inventory, false))
    }

    "equips a weapon, saying what it unequipped" in {
      val action = ItemActions.equip(Holding(scimitar, noted = false, Kind.Inventory), player, items)
      action.map(_.report) shouldBe Some("Equipped Rune scimitar")
      action.flatMap(_.detail) shouldBe Some("Unequipped Bronze sword")
      action.map(_.effects.size) shouldBe Some(2)
    }

    "only offers what suits the stack" in {
      ItemActions.canBank(Holding(book, noted = false, Kind.Inventory)) shouldBe false
      ItemActions.canBank(Holding(logs, noted = false, Kind.Bank)) shouldBe false
      ItemActions.canEquip(Holding(scimitar, noted = true, Kind.Inventory)) shouldBe false
      ItemActions.canEquip(Holding(scimitar, noted = false, EquipmentSlot.Weapon)) shouldBe false
      ItemActions.canWithdrawNoted(Holding(book, noted = false, Kind.Bank)) shouldBe false
    }

    "deposits the inventory, noted stacks as unnoted, leaving what can't be banked" in {
      val holding = player.copy(depositories =
        player.depositories + (Kind.Inventory -> Depository(Map((logs.id, true) -> 20, (book.id, false) -> 1), Kind.Inventory))
      )
      val action = ItemActions.depositInventory(holding, items)
      action.map(_.effects) shouldBe Some(List(DepositAll(DepositSource.Inventory)))
      action.flatMap(_.detail) shouldBe Some("1 stack at this step")
      ItemEffects.deposits(DepositSource.Inventory, holding, items) shouldBe
        List(MoveItem(logs.id, Exact(20), Kind.Inventory, notedInSource = true, Kind.Bank, noteInTarget = false))
    }

    "deposits equipment" in {
      ItemActions.depositEquipment(player, items).map(_.effects) shouldBe Some(List(DepositAll(DepositSource.Equipment)))
      ItemEffects.deposits(DepositSource.Equipment, player, items) shouldBe
        List(MoveItem(sword.id, Exact(1), EquipmentSlot.Weapon, notedInSource = false, Kind.Bank, noteInTarget = false))
    }

    "has nothing to deposit from empty places" in {
      ItemActions.depositEquipment(player.copy(depositories = Map.empty), items) shouldBe None
    }

    "describes quantities with thousands separators" in {
      ItemActions.describe(logs, Exact(1)) shouldBe "Logs"
      ItemActions.describe(logs, Exact(12500)) shouldBe "12,500 × Logs"
      ItemActions.describe(logs, ItemQuantity.Max) shouldBe "all Logs"
    }
  }
}
