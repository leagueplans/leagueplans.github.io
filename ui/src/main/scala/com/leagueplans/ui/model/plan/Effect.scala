package com.leagueplans.ui.model.plan

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.common.model.{Item, Skill}
import com.leagueplans.ui.model.player.item.Depository
import com.leagueplans.ui.model.player.skill.Exp

// Saved plans refer to effects by their position in this list, so new effects go at the end

enum Effect {
  /** A number of actions, each giving the same exp before the multiplier. A gain of a set amount of
    * exp is a single action. */
  case GainExp(skill: Skill, actions: Int, expEach: Exp)

  /** Adds items, or takes them away */
  case AddItem(item: Item.ID, change: ItemChange, target: Depository.Kind, note: Boolean)
  case MoveItem(
    item: Item.ID,
    quantity: ItemQuantity,
    source: Depository.Kind,
    notedInSource: Boolean,
    target: Depository.Kind,
    noteInTarget: Boolean
  )

  case UnlockSkill(skill: Skill)

  case CompleteQuest(quest: Int)
  case CompleteDiaryTask(task: Int)
  case CompleteLeagueTask(task: Int)
  case CompleteGridTile(tile: Int)

  /** Banks everything the inventory, or every equipment slot, holds when the effect applies, apart from
    * items that can't be banked */
  case DepositAll(source: Effect.DepositSource)

  /** Sets a bank PIN, which unlocks 20 more bank slots */
  case SetBankPin

  /** Buys a block of 50 bank slots from a banker. There are nine, bought in order and numbered
    * from 1, each for a set price in coins. The coins come from the inventory if it holds enough,
    * and otherwise from the bank, never from both. */
  case BuyBankSpace(block: Int)
}

object Effect {
  enum DepositSource {
    case Inventory, Equipment

    /** The places a deposit banks from */
    def places: List[Depository.Kind] =
      this match {
        case Inventory => List(Depository.Kind.Inventory)
        case Equipment => Depository.Kind.EquipmentSlot.values.toList
      }
  }

  object DepositSource {
    given Encoder[DepositSource] = Encoder.derived
    given Decoder[DepositSource] = Decoder.derived
  }

  extension (gain: GainExp) {
    /** The exp before the multiplier */
    def baseExp: Exp =
      gain.expEach * gain.actions
  }

  given Encoder[Effect] = Encoder.derived
  given Decoder[Effect] = Decoder.derived
}
