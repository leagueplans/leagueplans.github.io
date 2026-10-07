package com.leagueplans.ui.model.player.item

import cats.data.NonEmptyList
import com.leagueplans.common.model.{EquipmentType, InfoboxKey, Item}
import com.leagueplans.ui.model.plan.ItemChange
import com.leagueplans.ui.model.plan.Effect.{AddItem, BuyBankSpace, DepositAll, DepositSource, MoveItem, SetBankPin}
import com.leagueplans.ui.model.plan.ItemQuantity.Max
import com.leagueplans.ui.model.player.item.Depository.Kind
import com.leagueplans.ui.model.player.item.Depository.Kind.EquipmentSlot
import com.leagueplans.ui.model.player.league.LeagueStatus
import com.leagueplans.ui.model.player.skill.Stats
import com.leagueplans.ui.model.player.{GridStatus, Player}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class ItemEffectsTest extends AnyFreeSpec with Matchers {
  private def item(
    id: Int,
    stackable: Boolean = false,
    bankable: Item.Bankable = Item.Bankable.Yes(stacks = true),
    equipmentType: Option[EquipmentType] = None
  ): Item =
    Item(
      Item.ID(id),
      gameID = None,
      s"Item $id",
      examine = "",
      NonEmptyList.one((Item.Image.Bin(1), Item.Image.Path("1/1.png"))),
      bankable,
      stackable,
      noteable = true,
      equipmentType,
      infobox = InfoboxKey(1, List.empty)
    )

  private val lobster = item(1)
  private val coins = item(2, stackable = true)
  private val book = item(3, bankable = Item.Bankable.No)
  private val scimitar = item(4, equipmentType = Some(EquipmentType.Weapon))
  private val gold = item(BankSpace.coins, stackable = true)
  private val items = List(lobster, coins, book, scimitar, gold).map(i => i.id -> i).toMap

  private def player(contents: ((Kind, Item, Boolean), Int)*): Player =
    Player(
      Stats(),
      contents
        .groupBy { case ((kind, _, _), _) => kind }
        .map((kind, entries) =>
          kind -> Depository(entries.map { case ((_, item, noted), n) => (item.id, noted) -> n }.toMap, kind)
        ),
      Set.empty,
      Set.empty,
      LeagueStatus(0, Set.empty, Set.empty),
      GridStatus(Set.empty)
    )

  private def held(player: Player, kind: Kind, item: Item, noted: Boolean = false): Int =
    player.get(kind).count(item.id, noted)

  "ItemEffects" - {
    "moves all of a stack that's held where the effect applies" in {
      val before = player(((Kind.Inventory, coins, false), 12345))
      val after = ItemEffects(before, MoveItem(coins.id, Max, Kind.Inventory, false, Kind.Bank, false), items)
      held(after, Kind.Bank, coins) shouldBe 12345
      held(after, Kind.Inventory, coins) shouldBe 0
    }

    "withdraws all of an unstacked item only as far as the inventory has room" in {
      val before = player(((Kind.Bank, lobster, false), 300), ((Kind.Inventory, book, false), 20))
      val withdrawAll: MoveItem = MoveItem(lobster.id, Max, Kind.Bank, false, Kind.Inventory, false)
      ItemEffects.count(withdrawAll, before, items) shouldBe 8
      val after = ItemEffects(before, withdrawAll, items)
      held(after, Kind.Inventory, lobster) shouldBe 8
      held(after, Kind.Bank, lobster) shouldBe 292
    }

    "moves everything held into a full place, apart from withdrawals, so the plan can show the problem" in {
      val before = player(((EquipmentSlot.Weapon, scimitar, false), 1), ((Kind.Inventory, lobster, false), 28))
      val unequip: MoveItem = MoveItem(scimitar.id, Max, EquipmentSlot.Weapon, false, Kind.Inventory, false)
      ItemEffects.count(unequip, before, items) shouldBe 1
      held(ItemEffects(before, unequip, items), Kind.Inventory, scimitar) shouldBe 1
    }

    "withdraws all of a noted stack, which takes one slot" in {
      val before = player(((Kind.Bank, lobster, false), 300), ((Kind.Inventory, book, false), 20))
      ItemEffects.count(MoveItem(lobster.id, Max, Kind.Bank, false, Kind.Inventory, true), before, items) shouldBe 300
    }

    "removes all that's held" in {
      val before = player(((Kind.Inventory, lobster, false), 6))
      ItemEffects(before, AddItem(lobster.id, ItemChange.Empty, Kind.Inventory, note = false), items).get(Kind.Inventory).contents shouldBe empty
    }

    "removes an exact amount" in {
      val before = player(((Kind.Inventory, coins, false), 100))
      held(ItemEffects(before, AddItem(coins.id, ItemChange.By(-30), Kind.Inventory, note = false), items), Kind.Inventory, coins) shouldBe 70
    }

    "fills the inventory's free slots" in {
      val before = player(((Kind.Inventory, book, false), 25))
      ItemEffects.count(AddItem(lobster.id, ItemChange.Fill, Kind.Inventory, note = false), before, items) shouldBe 3
    }

    "can't fill with a stackable item, which has no limit" in {
      ItemEffects.count(AddItem(coins.id, ItemChange.Fill, Kind.Inventory, note = false), player(), items) shouldBe 0
    }

    "comes to nothing when there's nothing to move" in {
      ItemEffects.count(MoveItem(coins.id, Max, Kind.Inventory, false, Kind.Bank, false), player(), items) shouldBe 0
    }

    "deposits everything bankable, unnoting notes, whatever is held where it applies" in {
      val before = player(((Kind.Inventory, lobster, true), 20), ((Kind.Inventory, book, false), 1), ((Kind.Inventory, coins, false), 50))
      val after = ItemEffects(before, DepositAll(DepositSource.Inventory), items)
      held(after, Kind.Bank, lobster) shouldBe 20
      held(after, Kind.Bank, coins) shouldBe 50
      after.get(Kind.Inventory).contents shouldBe Map((book.id, false) -> 1)
    }

    "deposits equipment" in {
      val before = player(((EquipmentSlot.Weapon, scimitar, false), 1))
      held(ItemEffects(before, DepositAll(DepositSource.Equipment), items), Kind.Bank, scimitar) shouldBe 1
    }

    "has room for one unstackable item in an empty equipment slot, and none in a full one" in {
      ItemEffects.room(scimitar, noted = false, EquipmentSlot.Weapon, player(), items) shouldBe Some(1)
      ItemEffects.room(scimitar, noted = false, EquipmentSlot.Weapon, player(((EquipmentSlot.Weapon, scimitar, false), 1)), items) shouldBe Some(0)
    }

    "counts a slot for each item that doesn't stack in the bank" in {
      val flask = item(5000, bankable = Item.Bankable.Yes(stacks = false))
      val fillers = (6000 until 6897).map(item(_))
      val allItems = items ++ (flask +: fillers).map(i => i.id -> i)
      val bank = Depository(fillers.map(i => (i.id, false) -> 1).toMap + ((flask.id, false) -> 3), Kind.Bank)
      val full = player().copy(depositories = Map(Kind.Bank -> bank))

      // 897 stacks and 3 flasks fill all 900 slots
      ItemEffects.room(lobster, noted = false, Kind.Bank, full, allItems) shouldBe Some(0)
      ItemEffects.room(flask, noted = false, Kind.Bank, full, allItems) shouldBe Some(0)
      // With a PIN's 20 slots, each further flask needs a slot of its own
      ItemEffects.room(flask, noted = false, Kind.Bank, full.copy(bankSpace = BankSpace(Set(BankSpace.Unlock.Pin), 0)), allItems) shouldBe Some(20)
    }

    "sets a bank PIN, unlocking its space" in {
      ItemEffects(player(), SetBankPin, items).bankSpace.unlocks shouldBe Set(BankSpace.Unlock.Pin)
    }

    "pays for bank space from the inventory if it holds enough" in {
      val after = ItemEffects(player(((Kind.Inventory, gold, false), 3000000), ((Kind.Bank, gold, false), 5000000)), BuyBankSpace(2), items)
      held(after, Kind.Inventory, gold) shouldBe 1000000
      held(after, Kind.Bank, gold) shouldBe 5000000
      after.bankSpace.blocksBought shouldBe 1
    }

    "pays for bank space from the bank if only it holds enough, never from both" in {
      val split = player(((Kind.Inventory, gold, false), 600000), ((Kind.Bank, gold, false), 600000))
      val fromBank = ItemEffects(player(((Kind.Inventory, gold, false), 600000), ((Kind.Bank, gold, false), 1000000)), BuyBankSpace(1), items)
      held(fromBank, Kind.Inventory, gold) shouldBe 600000
      held(fromBank, Kind.Bank, gold) shouldBe 0

      // Neither holds enough alone: the slots are still gained, and the plan shows the problem
      val unpaid = ItemEffects(split, BuyBankSpace(1), items)
      unpaid.get(Kind.Inventory) shouldBe split.get(Kind.Inventory)
      unpaid.get(Kind.Bank) shouldBe split.get(Kind.Bank)
      unpaid.bankSpace.blocksBought shouldBe 1
    }

    "buys nothing for a block that's already bought" in {
      val bought = player(((Kind.Bank, gold, false), 5000000)).copy(bankSpace = BankSpace(Set.empty, blocksBought = 2))
      ItemEffects(bought, BuyBankSpace(2), items) shouldBe bought
    }

    "buys nothing for a block that doesn't exist" in {
      ItemEffects(player(((Kind.Inventory, gold, false), 1000000)), BuyBankSpace(10), items) shouldBe
        player(((Kind.Inventory, gold, false), 1000000))
    }

    "has room in the bank for a new stack only while the player's bank space has a free slot" in {
      val fillers = (1000 until 1900).map(item(_))
      val allItems = items ++ fillers.map(i => i.id -> i)
      val fullBank = player().copy(depositories = Map(
        Kind.Bank -> Depository(fillers.map(i => (i.id, false) -> 1).toMap, Kind.Bank)
      ))
      ItemEffects.room(lobster, noted = false, Kind.Bank, fullBank, allItems) shouldBe Some(0)
      ItemEffects.room(lobster, noted = false, Kind.Bank, fullBank.copy(bankSpace = BankSpace(Set(BankSpace.Unlock.Pin), 0)), allItems) shouldBe None
    }
  }
}
