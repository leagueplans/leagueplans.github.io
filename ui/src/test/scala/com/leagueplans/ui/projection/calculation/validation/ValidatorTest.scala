package com.leagueplans.ui.projection.calculation.validation

import cats.data.NonEmptyList
import com.leagueplans.common.model.{EquipmentType, InfoboxKey, Item}
import com.leagueplans.ui.model.plan.Effect.{AddItem, DepositSource, MoveItem}
import com.leagueplans.ui.model.plan.ItemChange
import com.leagueplans.ui.model.plan.ItemQuantity.{Exact, Max}
import com.leagueplans.ui.model.player.item.Depository
import com.leagueplans.ui.model.player.item.Depository.Kind
import com.leagueplans.ui.model.player.item.Depository.Kind.EquipmentSlot
import com.leagueplans.ui.model.player.league.LeagueStatus
import com.leagueplans.ui.model.player.skill.Stats
import com.leagueplans.ui.model.player.{Cache, GridStatus, Player}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class ValidatorTest extends AnyFreeSpec with Matchers {
  private def item(
    id: Int,
    name: String,
    stackable: Boolean = false,
    noteable: Boolean = true,
    bankable: Item.Bankable = Item.Bankable.Yes(stacks = true),
    equipmentType: Option[EquipmentType] = None
  ): Item =
    Item(
      Item.ID(id),
      gameID = None,
      name,
      examine = "",
      NonEmptyList.one((Item.Image.Bin(1), Item.Image.Path("1/1.png"))),
      bankable,
      stackable,
      noteable,
      equipmentType,
      infobox = InfoboxKey(1, List.empty)
    )

  private val logs = item(1, "Logs")
  private val coins = item(2, "Coins", stackable = true, noteable = false)
  private val scimitar = item(3, "Rune scimitar", equipmentType = Some(EquipmentType.Weapon))
  private val axe = item(4, "Rune axe", equipmentType = Some(EquipmentType.Weapon))
  private val bronzeArrows = item(5, "Bronze arrow", stackable = true, noteable = false, equipmentType = Some(EquipmentType.Ammo))
  private val ironArrows = item(6, "Iron arrow", stackable = true, noteable = false, equipmentType = Some(EquipmentType.Ammo))
  private val present = item(7, "A big present", bankable = Item.Bankable.No, noteable = false)
  private val book = item(8, "Book", noteable = false)
  private val cache =
    Cache(List(logs, coins, scimitar, axe, bronzeArrows, ironArrows, present, book).map(i => i.id -> i).toMap, Map.empty, Map.empty, Map.empty, Map.empty)

  private def player(contents: ((Depository.Kind, Item, Boolean), Int)*): Player =
    Player(
      Stats(),
      contents
        .groupBy { case ((kind, _, _), _) => kind }
        .map((kind, entries) => kind -> Depository(entries.map { case ((_, i, noted), n) => (i.id, noted) -> n }.toMap, kind)),
      Set.empty,
      Set.empty,
      LeagueStatus(0, Set.empty, Set.empty),
      GridStatus(Set.empty)
    )

  private def error(validator: Validator, player: Player): String =
    validator(player, None, cache).left.getOrElse(fail("Expected a problem"))

  "Validator" - {
    "says how many of an item a place holds when it's short" in {
      error(Validator.hasItem(Kind.Inventory, logs.id, noted = false, 5), player()) shouldBe
        "The inventory has no Logs at this step, short of 5"
      error(Validator.hasItem(Kind.Inventory, logs.id, noted = false, 5), player(((Kind.Inventory, logs, false), 2))) shouldBe
        "The inventory has 2 × Logs at this step, short of 5"
      error(Validator.hasItem(EquipmentSlot.Shield, scimitar.id, noted = false, 1), player()) shouldBe
        "The shield slot has no Rune scimitar at this step"
      error(Validator.hasItem(Kind.Inventory, logs.id, noted = true, 3), player()) shouldBe
        "The inventory has no Logs (noted) at this step, short of 3"
    }

    "says what a place lacks for the most of an item" in {
      error(Validator.allComesToSome(AddItem(coins.id, ItemChange.Empty, Kind.Inventory, note = false)), player()) shouldBe
        "The inventory has no Coins at this step"
      error(Validator.allComesToSome(MoveItem(scimitar.id, Max, EquipmentSlot.Weapon, false, Kind.Inventory, false)), player()) shouldBe
        "The weapon slot has no Rune scimitar at this step"
      val full = player(((Kind.Inventory, book, false), 28), ((Kind.Bank, logs, false), 5))
      error(Validator.allComesToSome(AddItem(logs.id, ItemChange.Fill, Kind.Inventory, note = false)), full) shouldBe
        "The inventory has no room for Logs at this step"
      error(Validator.allComesToSome(MoveItem(logs.id, Max, Kind.Bank, false, Kind.Inventory, false)), full) shouldBe
        "The inventory has no room for Logs at this step"
    }

    "says which items can be added until full" in {
      val message = "can't be added until full: only unnoted items that each take an inventory slot can"
      error(Validator.allComesToSome(AddItem(coins.id, ItemChange.Fill, Kind.Inventory, note = false)), player()) shouldBe
        s"Coins $message"
      error(Validator.allComesToSome(AddItem(logs.id, ItemChange.Fill, Kind.Inventory, note = true)), player()) shouldBe
        s"Logs (noted) $message"
      error(Validator.allComesToSome(AddItem(logs.id, ItemChange.Fill, Kind.Bank, note = false)), player(((Kind.Bank, logs, false), 1))) shouldBe
        s"Logs $message"
    }

    "words an overfull place for what it holds" in {
      error(Validator.depositorySize(Kind.Inventory), player(((Kind.Inventory, book, false), 30))) shouldBe
        "This would fill 30 inventory slots, but there are only 28"
      error(Validator.depositorySize(EquipmentSlot.Weapon), player(((EquipmentSlot.Weapon, scimitar, false), 1), ((EquipmentSlot.Weapon, axe, false), 1))) shouldBe
        "This would put Rune axe and Rune scimitar in the weapon slot, which holds one item"
      error(Validator.depositorySize(EquipmentSlot.Weapon), player(((EquipmentSlot.Weapon, scimitar, false), 2))) shouldBe
        "This would put 2 × Rune scimitar in the weapon slot, which holds one item"
      error(Validator.depositorySize(EquipmentSlot.Ammo), player(((EquipmentSlot.Ammo, bronzeArrows, false), 50), ((EquipmentSlot.Ammo, ironArrows, false), 20))) shouldBe
        "This would put Bronze arrow and Iron arrow in the ammo slot, which holds one stack"
    }

    "gives the reason an item can't be moved somewhere" in {
      def route(move: MoveItem) = error(Validator.possibleRoute(move), player())
      route(MoveItem(present.id, Exact(1), Kind.Inventory, false, Kind.Bank, false)) shouldBe "A big present can't be banked"
      route(MoveItem(logs.id, Exact(1), Kind.Bank, false, EquipmentSlot.Head, false)) shouldBe "Logs can't be equipped"
      route(MoveItem(scimitar.id, Exact(1), Kind.Bank, false, EquipmentSlot.Head, false)) shouldBe
        "Rune scimitar can't be equipped in the head slot"
      route(MoveItem(scimitar.id, Exact(1), Kind.Inventory, true, EquipmentSlot.Weapon, false)) shouldBe
        "Noted Rune scimitar can't be equipped"
      route(MoveItem(coins.id, Exact(1), Kind.Bank, false, Kind.Inventory, true)) shouldBe "Coins can't be noted"
      route(MoveItem(scimitar.id, Exact(1), EquipmentSlot.Weapon, false, Kind.Inventory, true)) shouldBe
        "Rune scimitar can't be moved from the weapon slot to the inventory (noted)"
    }

    "says when there's nothing to deposit" in {
      error(Validator.somethingToDeposit(DepositSource.Inventory), player()) shouldBe "The inventory has nothing to bank at this step"
      error(Validator.somethingToDeposit(DepositSource.Equipment), player()) shouldBe "Nothing equipped can be banked at this step"
    }
  }
}
