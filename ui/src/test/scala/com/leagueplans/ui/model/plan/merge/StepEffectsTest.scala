package com.leagueplans.ui.model.plan.merge

import cats.data.NonEmptyList
import com.leagueplans.common.model.{InfoboxKey, Item}
import com.leagueplans.ui.model.plan.{Effect, EffectList, ItemChange, ItemQuantity}
import com.leagueplans.ui.model.plan.Effect.{AddItem, DepositAll, DepositSource, MoveItem}
import com.leagueplans.ui.model.plan.ItemQuantity.{Exact, Max}
import com.leagueplans.ui.model.player.item.Depository
import com.leagueplans.ui.model.player.item.Depository.Kind
import com.leagueplans.ui.model.player.item.Depository.Kind.EquipmentSlot
import com.leagueplans.ui.model.player.league.LeagueStatus
import com.leagueplans.ui.model.player.skill.Stats
import com.leagueplans.ui.model.player.{GridStatus, Player}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class StepEffectsTest extends AnyFreeSpec with Matchers {
  private val logs = Item.ID(1)
  private val lobster = Item.ID(2)
  private val arrows = Item.ID(3)

  private val items: Map[Item.ID, Item] =
    List(logs -> false, lobster -> false, arrows -> true).map((id, stackable) =>
      id -> Item(
        id,
        gameID = None,
        s"Item $id",
        examine = "",
        NonEmptyList.one((Item.Image.Bin(1), Item.Image.Path("1/1.png"))),
        Item.Bankable.Yes(stacks = true),
        stackable,
        noteable = !stackable,
        equipmentType = None,
        infobox = InfoboxKey(1, List.empty)
      )
    ).toMap

  private def move(item: Item.ID, quantity: ItemQuantity, source: Depository.Kind, target: Depository.Kind): MoveItem =
    MoveItem(item, quantity, source, notedInSource = false, target, noteInTarget = false)

  private def withdraw(quantity: ItemQuantity, item: Item.ID = logs): MoveItem = move(item, quantity, Kind.Bank, Kind.Inventory)
  private def bank(quantity: ItemQuantity, item: Item.ID = logs): MoveItem = move(item, quantity, Kind.Inventory, Kind.Bank)
  private def add(change: ItemChange, item: Item.ID = logs): AddItem = AddItem(item, change, Kind.Inventory, note = false)

  private def player(contents: ((Depository.Kind, Item.ID), Int)*): Player =
    Player(
      Stats(),
      contents
        .groupBy { case ((kind, _), _) => kind }
        .map((kind, entries) => kind -> Depository(entries.map { case ((_, item), n) => (item, false) -> n }.toMap, kind)),
      Set.empty,
      Set.empty,
      LeagueStatus(0, Set.empty, Set.empty),
      GridStatus(Set.empty)
    )

  private def merged(effects: Effect*): List[Effect] =
    effects.foldLeft(EffectList.empty)(StepEffects.add(_, _, playerAtStart = None, items)).underlying

  "StepEffects" - {
    "adds up exact amounts of the same move" in {
      merged(withdraw(Exact(2)), withdraw(Exact(3))) shouldBe List(withdraw(Exact(5)))
    }

    "cancels out exact moves the opposite way" in {
      merged(withdraw(Exact(5)), bank(Exact(2))) shouldBe List(withdraw(Exact(3)))
      merged(withdraw(Exact(2)), bank(Exact(2))) shouldBe List.empty
    }

    "adds up exact additions and removals, which cancel out at nothing" in {
      merged(add(ItemChange.By(2)), add(ItemChange.By(3))) shouldBe List(add(ItemChange.By(5)))
      merged(add(ItemChange.By(5)), add(ItemChange.By(-2))) shouldBe List(add(ItemChange.By(3)))
      merged(add(ItemChange.By(5)), add(ItemChange.By(-5))) shouldBe List.empty
    }

    "lets a move of everything take the place of earlier moves between the same places, either way" in {
      merged(withdraw(Exact(2)), withdraw(Max)) shouldBe List(withdraw(Max))
      merged(withdraw(Exact(5)), bank(Max)) shouldBe List(bank(Max))
      val equip = move(arrows, Max, Kind.Bank, EquipmentSlot.Ammo)
      val unequip = move(arrows, Max, EquipmentSlot.Ammo, Kind.Bank)
      merged(equip, unequip, equip, unequip, equip) shouldBe List(equip)
    }

    "lets emptying a place take the place of earlier changes to it" in {
      merged(add(ItemChange.By(3)), add(ItemChange.Fill), add(ItemChange.Empty)) shouldBe List(add(ItemChange.Empty))
    }

    "lets a deposit take the place of earlier moves between the place and the bank" in {
      val equip = move(arrows, Exact(10), Kind.Bank, EquipmentSlot.Ammo)
      merged(withdraw(Exact(5)), equip, DepositAll(DepositSource.Inventory)) shouldBe
        List(equip, DepositAll(DepositSource.Inventory))
      merged(DepositAll(DepositSource.Inventory), Effect.CompleteQuest(1), DepositAll(DepositSource.Inventory)) shouldBe
        List(DepositAll(DepositSource.Inventory), Effect.CompleteQuest(1))
    }

    "lets filling a place take the place of earlier changes to it, for items that take a slot each" in {
      merged(add(ItemChange.By(-2)), add(ItemChange.Fill)) shouldBe List(add(ItemChange.Fill))
      merged(add(ItemChange.Empty), add(ItemChange.Fill)) shouldBe List(add(ItemChange.Fill))
      // Filling with a stackable item adds nothing, so the removal still stands
      merged(add(ItemChange.By(-2), arrows), add(ItemChange.Fill, arrows)) shouldBe
        List(add(ItemChange.By(-2), arrows), add(ItemChange.Fill, arrows))
    }

    "keeps the later choice between an exact amount and the most" in {
      merged(withdraw(Max), withdraw(Exact(2))) shouldBe List(withdraw(Exact(2)))
      merged(add(ItemChange.By(2)), add(ItemChange.Fill)) shouldBe List(add(ItemChange.Fill))
      merged(add(ItemChange.Fill), add(ItemChange.By(2))) shouldBe List(add(ItemChange.By(2)))
      merged(add(ItemChange.Empty), add(ItemChange.By(-4))) shouldBe List(add(ItemChange.By(-4)))
    }

    "doesn't merge past an effect the later one depends on" - {
      "equipping again after unequipping everything" in {
        val equip = move(arrows, Exact(10), Kind.Bank, EquipmentSlot.Ammo)
        val unequip = move(arrows, Max, EquipmentSlot.Ammo, Kind.Bank)
        merged(equip, unequip, equip) shouldBe List(unequip, equip)
      }

      "withdrawing after depositing everything" in {
        merged(DepositAll(DepositSource.Inventory), withdraw(Exact(5)), DepositAll(DepositSource.Inventory)) shouldBe
          List(DepositAll(DepositSource.Inventory))
        merged(withdraw(Exact(5)), DepositAll(DepositSource.Inventory), withdraw(Exact(3))) shouldBe
          List(DepositAll(DepositSource.Inventory), withdraw(Exact(3)))
      }

      "a fill, which depends on the inventory's space" in {
        merged(withdraw(Exact(5)), withdraw(Max, lobster), withdraw(Exact(3))) shouldBe
          List(withdraw(Exact(5)), withdraw(Max, lobster), withdraw(Exact(3)))
      }

      "a move that supplies what the later one takes" in {
        // Banking 10 equipped arrows relies on the equip before it
        val equip = move(arrows, Max, Kind.Inventory, EquipmentSlot.Ammo)
        val bankSome = move(arrows, Exact(10), EquipmentSlot.Ammo, Kind.Bank)
        val unequip = move(arrows, Max, EquipmentSlot.Ammo, Kind.Inventory)
        merged(equip, bankSome, unequip) shouldBe List(equip, bankSome, unequip)
      }

      "a removal that frees the space the later one takes" in {
        merged(withdraw(Exact(2)), add(ItemChange.By(-3), lobster), withdraw(Exact(3))) shouldBe
          List(withdraw(Exact(2)), add(ItemChange.By(-3), lobster), withdraw(Exact(3)))
      }
    }

    "keeps different directions apart, as in banking all but two" in {
      merged(bank(Max), withdraw(Exact(2))) shouldBe List(bank(Max), withdraw(Exact(2)))
      merged(add(ItemChange.Fill), add(ItemChange.By(-2))) shouldBe List(add(ItemChange.Fill), add(ItemChange.By(-2)))
    }

    "when reconciled, merges effects that can now meet, without keeping later choices" in {
      val removeLobsters = add(ItemChange.By(-3), lobster)
      StepEffects.reconcile(EffectList(List(withdraw(Exact(2)), withdraw(Exact(3)))), playerAtStart = None, items).underlying shouldBe List(withdraw(Exact(5)))
      // Deleting what separated a fill from an exact addition doesn't replace the fill
      StepEffects.reconcile(EffectList(List(add(ItemChange.Fill), add(ItemChange.By(2)))), playerAtStart = None, items).underlying shouldBe
        List(add(ItemChange.Fill), add(ItemChange.By(2)))
      StepEffects.reconcile(EffectList(List(withdraw(Exact(2)), removeLobsters, withdraw(Exact(3)))), playerAtStart = None, items).underlying shouldBe
        List(withdraw(Exact(2)), removeLobsters, withdraw(Exact(3)))
    }

    "merges past effects that don't touch what the later one does" in {
      merged(withdraw(Exact(2)), withdraw(Exact(4), lobster), Effect.CompleteQuest(1), withdraw(Exact(3))) shouldBe
        List(withdraw(Exact(5)), withdraw(Exact(4), lobster), Effect.CompleteQuest(1))
    }
    "drops what a merge leaves that comes to nothing at the step" - {
      val equip = move(arrows, Max, Kind.Bank, EquipmentSlot.Ammo)
      val unequip = move(arrows, Max, EquipmentSlot.Ammo, Kind.Bank)
      def added(start: Player, effects: Effect*) =
        effects.foldLeft(EffectList.empty)(StepEffects.add(_, _, Some(start), items)).underlying

      "when an effect is added" in {
        // Nothing was equipped, so unequipping everything equipped does nothing
        added(player((Kind.Bank, arrows) -> 100), equip, unequip) shouldBe empty
        // Something was, so it still unequips that
        added(player((Kind.Bank, arrows) -> 100, (EquipmentSlot.Ammo, arrows) -> 5), equip, unequip) shouldBe List(unequip)
      }

      "but keeps an effect that comes to nothing when it's added on its own" in {
        added(player(), unequip) shouldBe List(unequip)
      }

      "when reconciled, but not what was already there, nor without the player at the start" in {
        val start = player((Kind.Bank, arrows) -> 100)
        // As after deleting what separated the equip from the unequip
        StepEffects.reconcile(EffectList(List(equip, unequip)), Some(start), items).underlying shouldBe empty
        StepEffects.reconcile(EffectList(List(unequip)), Some(start), items).underlying shouldBe List(unequip)
        StepEffects.reconcile(EffectList(List(equip, unequip)), None, items).underlying shouldBe List(unequip)
      }
    }
  }
}
