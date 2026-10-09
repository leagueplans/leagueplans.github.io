package com.leagueplans.ui.model.plan.merge

import com.leagueplans.common.model.Item
import com.leagueplans.ui.model.plan.{Effect, ItemChange}
import com.leagueplans.ui.model.plan.Effect.*
import com.leagueplans.ui.model.plan.ItemQuantity.{Exact, Max}
import com.leagueplans.ui.model.player.Player
import com.leagueplans.ui.model.player.item.{Depository, EquipPlan, ItemEffects}

/** Whether two effects that sit next to each other make a single change, and what it is:
  *
  *  - Repeating an exact change adds up, and doing the opposite takes it back, down to nothing.
  *  - An effect worked out where it applies, such as a move of everything held, takes the place of
  *    the earlier changes it overrides.
  *  - Changing an amount between exact and the most keeps the later choice, when asked to.
  */
private[merge] object MergeRules {
  enum Merged {
    /** The later effect stands in for both, so it stays where it was added */
    case Later
    /** The two become these effects where the earlier was: usually one, or none if they cancel
      * out, or the items equipping took off if the item was taken off again */
    case Into(effects: List[Effect])
  }

  /** Merges an effect with an earlier one that it sits next to, or None if they stay apart
    *
    * @param keepLatestChoice whether a later exact amount replaces an earlier most, or the most
    *                         an earlier exact amount, which changes what the step does
    * @param playerAt the player where the earlier effect applies, if it's known. Equipping and
    *                 taking the item off again only merge with it, since what equipping takes off
    *                 depends on what's worn.
    */
  def merge(
    earlier: Effect,
    later: Effect,
    items: Item.ID => Item,
    keepLatestChoice: Boolean,
    playerAt: Option[Player]
  ): Option[Merged] =
    (earlier, later) match {
      // Exp only merges when each action gives the same exp, so that the merged effect is still a
      // number of actions
      case (e: GainExp, l: GainExp) if e.skill == l.skill && e.expEach == l.expEach =>
        Some(Merged.Into(List(e.copy(actions = e.actions + l.actions))))

      case (e: AddItem, l: AddItem) if e.item == l.item && e.target == l.target && e.note == l.note =>
        (e.change, l.change) match {
          case (ItemChange.By(a), ItemChange.By(b)) =>
            Some(Merged.Into(Option.when(a + b != 0)(e.copy(change = ItemChange.By(a + b))).toList))
          // Emptying a place leaves none, and filling one leaves it full, whatever it held before.
          // Only items that take a slot each can fill a place: a fill of anything else adds nothing.
          case (_, ItemChange.Empty) =>
            Some(Merged.Later)
          case (_, ItemChange.Fill) if ItemEffects.canFill(items(l.item), l.note, l.target) =>
            Some(Merged.Later)
          case (ItemChange.Fill, ItemChange.Fill) =>
            Some(Merged.Later)
          case (ItemChange.By(n), ItemChange.Fill) if n > 0 && keepLatestChoice =>
            Some(Merged.Later)
          case (ItemChange.Fill, ItemChange.By(n)) if n > 0 && keepLatestChoice =>
            Some(Merged.Later)
          case (ItemChange.Empty, ItemChange.By(n)) if n < 0 && keepLatestChoice =>
            Some(Merged.Later)
          case _ =>
            None
        }

      // Equipping an unstackable item again swaps it for the one just equipped, which changes
      // nothing. Anything else equipping the same unstackable item twice is left as it is.
      case (e: MoveItem, l: MoveItem) if e.item == l.item && sameWay(e, l) && EquipPlan.equips(l, items) && !items(l.item).stackable =>
        Option.when(
          e.quantity == Exact(1) && l.quantity == Exact(1) && EquipPlan.returnTo(l.source) == l.source
        )(Merged.Into(List(e)))

      // Equipping takes off what's in the way, which taking the item back off doesn't put back. So
      // equipping and unequipping leaves the moves for what was taken off, worked out where the
      // equip applies. They're only merged where that leaves the player as the two moves would.
      case (e: MoveItem, l: MoveItem) if e.item == l.item && oppositeWays(e, l) && (EquipPlan.equips(e, items) || EquipPlan.equips(l, items)) =>
        playerAt.flatMap { player =>
          val displaced = EquipPlan.displaced(e, player, items)
          opposite(e, l)
            .map {
              case Merged.Into(effects) => displaced ++ effects
              case Merged.Later => displaced :+ l
            }
            .filter(merged => sameResult(player, List(e, l), merged, items))
            .map(Merged.Into(_))
        }

      case (e: MoveItem, l: MoveItem) if e.item == l.item && sameWay(e, l) =>
        (e.quantity, l.quantity) match {
          case (Exact(a), Exact(b)) => Some(Merged.Into(List(e.copy(quantity = Exact(a + b)))))
          // Moving everything gives the same result however much was moved the same way before
          case (_, Max) => Some(Merged.Later)
          case (Max, Exact(_)) => Option.when(keepLatestChoice)(Merged.Later)
        }

      case (e: MoveItem, l: MoveItem) if e.item == l.item && oppositeWays(e, l) =>
        opposite(e, l)

      // A deposit banks everything, however much moved between the place and the bank before
      case (e: MoveItem, l: DepositAll) if betweenBankAnd(e, l.source) =>
        Some(Merged.Later)

      // Completions, unlocks and deposits do nothing more the second time
      case (e, l) if e == l && !l.isInstanceOf[GainExp | AddItem | MoveItem] =>
        Some(Merged.Into(List(e)))

      case _ =>
        None
    }

  /** Merges moves of an item the opposite ways */
  private def opposite(e: MoveItem, l: MoveItem): Option[Merged] =
    (e.quantity, l.quantity) match {
      case (Exact(a), Exact(b)) =>
        Some(Merged.Into(
          if (a > b) List(e.copy(quantity = Exact(a - b)))
          else Option.when(b > a)(l.copy(quantity = Exact(b - a))).toList
        ))
      // Moving everything back ends the same however much went the other way first
      case (_, Max) => Some(Merged.Later)
      case (Max, Exact(_)) => None
    }

  private def sameResult(player: Player, effects: List[MoveItem], merged: List[Effect], items: Item.ID => Item): Boolean = {
    def held(effects: List[Effect]) =
      effects
        .foldLeft(player) {
          case (p, move: MoveItem) => ItemEffects(p, move, items)
          case (p, _) => p
        }
        .depositories.view.mapValues(_.contents).filter(_._2.nonEmpty).toMap
    merged.forall(_.isInstanceOf[MoveItem]) && held(effects) == held(merged)
  }


  private def sameWay(a: MoveItem, b: MoveItem): Boolean =
    a.source == b.source && a.notedInSource == b.notedInSource && a.target == b.target && a.noteInTarget == b.noteInTarget

  private def oppositeWays(a: MoveItem, b: MoveItem): Boolean =
    a.source == b.target && a.notedInSource == b.noteInTarget && a.target == b.source && a.noteInTarget == b.notedInSource

  private def betweenBankAnd(move: MoveItem, source: DepositSource): Boolean =
    (source.places.contains(move.source) && move.target == Depository.Kind.Bank) ||
      (move.source == Depository.Kind.Bank && source.places.contains(move.target))
}
