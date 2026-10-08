package com.leagueplans.ui.model.player.item

import cats.data.NonEmptyList
import com.leagueplans.common.model.{EquipmentType, InfoboxKey, Item}
import com.leagueplans.ui.model.plan.Effect.MoveItem
import com.leagueplans.ui.model.plan.ItemQuantity
import com.leagueplans.ui.model.plan.ItemQuantity.Exact
import com.leagueplans.ui.model.player.item.Depository.Kind
import com.leagueplans.ui.model.player.item.Depository.Kind.EquipmentSlot
import com.leagueplans.ui.model.player.item.ItemActions.Holding
import com.leagueplans.ui.model.player.item.ItemTransfer.{Quantity, Rejection, Settings, Target}
import com.leagueplans.ui.model.player.league.LeagueStatus
import com.leagueplans.ui.model.player.skill.Stats
import com.leagueplans.ui.model.player.{GridStatus, Player}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class ItemTransferTest extends AnyFreeSpec with Matchers {
  private def item(
    id: Int,
    stackable: Boolean = false,
    noteable: Boolean = true,
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
      noteable,
      equipmentType,
      infobox = InfoboxKey(1, List.empty)
    )

  private val lobster = item(1)
  private val coins = item(2, stackable = true, noteable = false)
  private val scimitar = item(3, equipmentType = Some(EquipmentType.Weapon))
  private val book = item(4, noteable = false, bankable = Item.Bankable.No)
  private val tinderbox = item(5, noteable = false)
  private val items = List(lobster, coins, scimitar, book, tinderbox).map(i => i.id -> i).toMap

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

  private def effects(
    source: Holding,
    target: Target,
    player: Player,
    settings: Settings = Settings.default
  ): Either[Rejection, List[com.leagueplans.ui.model.plan.Effect]] =
    ItemTransfer.plan(source, target, player, items, settings).map(_.effects)

  private val bankedLobsters = Holding(lobster, noted = false, Kind.Bank)

  "ItemTransfer" - {
    "does nothing when a stack is dropped back where it came from" in {
      effects(bankedLobsters, Target.Bank, player(((Kind.Bank, lobster, false), 5))) shouldBe Left(Rejection.SameDepository)
    }

    "withdraws the chosen quantity, up to what's held" in {
      val banked = player(((Kind.Bank, lobster, false), 7))
      def withdrawn(quantity: Quantity) =
        effects(bankedLobsters, Target.Inventory, banked, Settings(quantity, withdrawNoted = false))
          .map(_.collect { case move: MoveItem => move.quantity })

      withdrawn(Quantity.One) shouldBe Right(List(Exact(1)))
      withdrawn(Quantity.Five) shouldBe Right(List(Exact(5)))
      withdrawn(Quantity.Ten) shouldBe Right(List(Exact(7)))
      withdrawn(Quantity.X(6)) shouldBe Right(List(Exact(6)))
      withdrawn(Quantity.All) shouldBe Right(List(ItemQuantity.Max))
    }

    "withdraws notes when asked, unless the item can't be noted" in {
      effects(bankedLobsters, Target.Inventory, player(((Kind.Bank, lobster, false), 3)), Settings(Quantity.All, withdrawNoted = true)) shouldBe
        Right(List(MoveItem(lobster.id, ItemQuantity.Max, Kind.Bank, notedInSource = false, Kind.Inventory, noteInTarget = true)))

      effects(Holding(tinderbox, noted = false, Kind.Bank), Target.Inventory, player(((Kind.Bank, tinderbox, false), 1)), Settings(Quantity.All, withdrawNoted = true)) shouldBe
        Right(List(MoveItem(tinderbox.id, ItemQuantity.Max, Kind.Bank, notedInSource = false, Kind.Inventory, noteInTarget = false)))
    }

    "withdraws what's asked for, even into a full inventory, so the plan can show the problem" in {
      val nearlyFull = player(((Kind.Bank, lobster, false), 10), ((Kind.Inventory, tinderbox, false), 25))
      effects(bankedLobsters, Target.Inventory, nearlyFull, Settings(Quantity.Ten, withdrawNoted = false))
        .map(_.collect { case move: MoveItem => move.quantity }) shouldBe Right(List(Exact(10)))
      // All stops at what fits where it applies
      effects(bankedLobsters, Target.Inventory, nearlyFull).map(_.collect { case move: MoveItem => move.quantity }) shouldBe
        Right(List(ItemQuantity.Max))

      val full = player(((Kind.Bank, coins, false), 100), ((Kind.Inventory, tinderbox, false), 28))
      effects(Holding(coins, noted = false, Kind.Bank), Target.Inventory, full).isRight shouldBe true
    }

    "banks noted inventory stacks as unnoted" in {
      effects(Holding(lobster, noted = true, Kind.Inventory), Target.Bank, player(((Kind.Inventory, lobster, true), 20))) shouldBe
        Right(List(MoveItem(lobster.id, ItemQuantity.Max, Kind.Inventory, notedInSource = true, Kind.Bank, noteInTarget = false)))
    }

    "banks one unnoted, unstackable inventory item at a time, whatever the quantity setting" in {
      effects(Holding(lobster, noted = false, Kind.Inventory), Target.Bank, player(((Kind.Inventory, lobster, false), 6))) shouldBe
        Right(List(MoveItem(lobster.id, Exact(1), Kind.Inventory, notedInSource = false, Kind.Bank, noteInTarget = false)))
      effects(Holding(coins, noted = false, Kind.Inventory), Target.Bank, player(((Kind.Inventory, coins, false), 60))) shouldBe
        Right(List(MoveItem(coins.id, ItemQuantity.Max, Kind.Inventory, notedInSource = false, Kind.Bank, noteInTarget = false)))
    }

    "refuses to bank items that can't be banked" in {
      effects(Holding(book, noted = false, Kind.Inventory), Target.Bank, player(((Kind.Inventory, book, false), 1))) shouldBe
        Left(Rejection.NotBankable)
    }

    "equips an item dropped anywhere on the equipment panel, from the inventory or the bank" in {
      effects(Holding(scimitar, noted = false, Kind.Bank), Target.Equipment, player(((Kind.Bank, scimitar, false), 1))) shouldBe
        Right(List(MoveItem(scimitar.id, Exact(1), Kind.Bank, notedInSource = false, EquipmentSlot.Weapon, noteInTarget = false)))
    }

    "puts whatever an item from the bank displaces into the bank" in {
      val sword = item(7, equipmentType = Some(EquipmentType.Weapon))
      val withSword = items + (sword.id -> sword)
      val before = player(((Kind.Bank, scimitar, false), 1), ((EquipmentSlot.Weapon, sword, false), 1))
      val effects =
        ItemTransfer.plan(Holding(scimitar, noted = false, Kind.Bank), Target.Equipment, before, withSword, Settings.default).map(_.effects)
      effects shouldBe Right(List(
        MoveItem(scimitar.id, Exact(1), Kind.Bank, notedInSource = false, EquipmentSlot.Weapon, noteInTarget = false)
      ))
      val after = effects.toOption.get.collect { case move: MoveItem => move }.foldLeft(before)(ItemEffects(_, _, withSword))
      after.get(Kind.Bank).contents shouldBe Map((sword.id, false) -> 1)
    }

    "equips the withdraw quantity of a stackable item from the bank" in {
      val arrows = item(6, stackable = true, noteable = false, equipmentType = Some(EquipmentType.Ammo))
      val arrowItems = items + (arrows.id -> arrows)
      ItemTransfer
        .plan(Holding(arrows, noted = false, Kind.Bank), Target.Equipment, player(((Kind.Bank, arrows, false), 250)), arrowItems, Settings(Quantity.Ten, withdrawNoted = false))
        .map(_.effects) shouldBe
        Right(List(MoveItem(arrows.id, Exact(10), Kind.Bank, notedInSource = false, EquipmentSlot.Ammo, noteInTarget = false)))
    }

    "refuses to equip noted items, or items without a slot" in {
      effects(Holding(scimitar, noted = true, Kind.Inventory), Target.Equipment, player(((Kind.Inventory, scimitar, true), 1))) shouldBe
        Left(Rejection.NotedEquip)
      effects(Holding(lobster, noted = false, Kind.Inventory), Target.Equipment, player(((Kind.Inventory, lobster, false), 1))) shouldBe
        Left(Rejection.NotEquippable)
    }

    "takes equipped items off into the inventory or the bank" in {
      val equipped = player(((EquipmentSlot.Weapon, scimitar, false), 1))
      val source = Holding(scimitar, noted = false, EquipmentSlot.Weapon)
      effects(source, Target.Inventory, equipped) shouldBe
        Right(List(MoveItem(scimitar.id, Exact(1), EquipmentSlot.Weapon, notedInSource = false, Kind.Inventory, noteInTarget = false)))
      effects(source, Target.Bank, equipped) shouldBe
        Right(List(MoveItem(scimitar.id, Exact(1), EquipmentSlot.Weapon, notedInSource = false, Kind.Bank, noteInTarget = false)))
    }

    "refuses to move a stack that isn't held where it was dragged from" in {
      effects(bankedLobsters, Target.Inventory, player()) shouldBe Left(Rejection.NothingToMove)
    }

    "quick-moves a whole stack, ignoring the quantity setting" in {
      val settings = Settings(Quantity.One, withdrawNoted = false)
      ItemTransfer.quickMove(Holding(lobster, noted = false, Kind.Inventory), player(((Kind.Inventory, lobster, false), 4)), items, settings)
        .map(_.effects) shouldBe
        Right(List(MoveItem(lobster.id, ItemQuantity.Max, Kind.Inventory, notedInSource = false, Kind.Bank, noteInTarget = false)))
    }

    "quick-moves equipped items to the bank" in {
      ItemTransfer.quickMove(Holding(scimitar, noted = false, EquipmentSlot.Weapon), player(((EquipmentSlot.Weapon, scimitar, false), 1)), items, Settings.default)
        .map(_.effects) shouldBe
        Right(List(MoveItem(scimitar.id, Exact(1), EquipmentSlot.Weapon, notedInSource = false, Kind.Bank, noteInTarget = false)))
    }
  }
}
