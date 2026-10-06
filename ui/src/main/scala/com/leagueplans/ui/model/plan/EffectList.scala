package com.leagueplans.ui.model.plan

import com.leagueplans.codec.decoding.CollectionDecoder
import com.leagueplans.codec.encoding.CollectionEncoder
import com.leagueplans.common.model.Item
import com.leagueplans.ui.model.plan.Effect.*
import com.leagueplans.ui.model.plan.ItemQuantity.{Exact, Max}
import com.leagueplans.ui.model.player.item.{Depository, ItemEffects}
import com.leagueplans.ui.model.player.skill.Exp

object EffectList {
  val empty: EffectList = EffectList(List.empty)
  
  given CollectionEncoder[EffectList] =
    CollectionEncoder
      .iterableOnceEncoder[List, Effect]
      .contramap(_.underlying)

  given CollectionDecoder[EffectList] =
    CollectionDecoder
      .iterableOnceDecoder[List, Effect]
      .map(EffectList.apply)

  /** An effect, and whether it came from merging others */
  final case class Entry(effect: Effect, merged: Boolean)

  /** Adds an entry to the end of the others, merging it with the nearest earlier effect it can,
    * looking back only as far as the new effect could move without changing the step's result */
  private def insert(entries: List[Entry], entry: Entry, items: Item.ID => Item, keepLatestChoice: Boolean): List[Entry] = {
    val effects = entries.map(_.effect)
    def canReach(i: Int): Boolean =
      effects.drop(i + 1).forall(EffectDependencies.canMoveBefore(entry.effect, _))

    effects.zipWithIndex.reverse
      .takeWhile((_, i) => canReach(i))
      .collectFirst(Function.unlift((existing, i) => merge(existing, entry.effect, items, keepLatestChoice).map((_, i))))
      match {
        case None =>
          entries :+ entry
        // The new effect stays at the end if the earlier one could move forward to meet it. It may
        // have freed space that what came between needed, say.
        case Some((Merged.Later, i)) if effects.drop(i + 1).forall(EffectDependencies.canMoveBefore(_, effects(i))) =>
          // The new effect may stand in for more of the earlier effects
          insert(entries.patch(i, Nil, 1), entry.copy(merged = true), items, keepLatestChoice)
        case Some((Merged.Later, i)) =>
          insert(entries.take(i), entry.copy(merged = true), items, keepLatestChoice) ++ entries.drop(i + 1)
        case Some((Merged.Into(merged), i)) =>
          // What's merged takes the earlier effect's place, where it may merge again. An earlier
          // effect that stands unchanged keeps its own mark.
          val before = entries.take(i)
          val mergedEntry = merged.map(effect => Entry(effect, merged = effect != effects(i) || entries(i).merged))
          mergedEntry.fold(before)(insert(before, _, items, keepLatestChoice)) ++ entries.drop(i + 1)
      }
  }

  private enum Merged {
    /** The later effect stands in for both, so it stays where it was added */
    case Later
    /** The two become one effect where the earlier was, or nothing if they cancel out */
    case Into(effect: Option[Effect])
  }

  /** Merges an effect with an earlier one that it sits next to, or None if they stay apart */
  private def merge(earlier: Effect, later: Effect, items: Item.ID => Item, keepLatestChoice: Boolean): Option[Merged] =
    (earlier, later) match {
      case (e: GainExp, l: GainExp) if e.skill == l.skill =>
        Some(Merged.Into(Some(e.copy(baseExp = e.baseExp + l.baseExp)).filter(_.baseExp != Exp(0))))

      case (e: AddItem, l: AddItem) if e.item == l.item && e.target == l.target && e.note == l.note =>
        (e.change, l.change) match {
          case (ItemChange.By(a), ItemChange.By(b)) =>
            Some(Merged.Into(Option.when(a + b != 0)(e.copy(change = ItemChange.By(a + b)))))
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

      case (e: MoveItem, l: MoveItem) if e.item == l.item && sameWay(e, l) =>
        (e.quantity, l.quantity) match {
          case (Exact(a), Exact(b)) => Some(Merged.Into(Some(e.copy(quantity = Exact(a + b)))))
          // Moving everything gives the same result however much was moved the same way before
          case (_, Max) => Some(Merged.Later)
          case (Max, Exact(_)) => Option.when(keepLatestChoice)(Merged.Later)
        }

      case (e: MoveItem, l: MoveItem) if e.item == l.item && oppositeWays(e, l) =>
        (e.quantity, l.quantity) match {
          case (Exact(a), Exact(b)) =>
            Some(Merged.Into(
              if (a > b) Some(e.copy(quantity = Exact(a - b)))
              else Option.when(b > a)(l.copy(quantity = Exact(b - a)))
            ))
          // Moving everything back ends the same however much went the other way first
          case (_, Max) => Some(Merged.Later)
          case (Max, Exact(_)) => None
        }

      // A deposit banks everything, however much moved between the place and the bank before
      case (e: MoveItem, l: DepositAll) if betweenBankAnd(e, l.source) =>
        Some(Merged.Later)

      // Completions, unlocks and deposits do nothing more the second time
      case (e, l) if e == l && !l.isInstanceOf[GainExp | AddItem | MoveItem] =>
        Some(Merged.Into(Some(e)))

      case _ =>
        None
    }


  private def sameWay(a: MoveItem, b: MoveItem): Boolean =
    a.source == b.source && a.notedInSource == b.notedInSource && a.target == b.target && a.noteInTarget == b.noteInTarget

  private def oppositeWays(a: MoveItem, b: MoveItem): Boolean =
    a.source == b.target && a.notedInSource == b.noteInTarget && a.target == b.source && a.noteInTarget == b.notedInSource

  private def betweenBankAnd(move: MoveItem, source: DepositSource): Boolean =
    (source.places.contains(move.source) && move.target == Depository.Kind.Bank) ||
      (move.source == Depository.Kind.Bank && source.places.contains(move.target))
}

/** A step's effects. Adding an effect merges it with an earlier one where the two make a single
  * change, so that a step reads as what it does rather than every click that made it:
  *
  *  - Repeating an exact change adds up, and doing the opposite takes it back, down to nothing.
  *  - An effect worked out where it applies, such as a move of everything held, takes the place of
  *    the earlier changes it overrides.
  *  - Changing an amount between exact and the most keeps the later choice.
  *
  * An effect only merges with an earlier one if it can move back next to it without changing what
  * the step does, which `EffectDependencies` decides.
  */
final case class EffectList(underlying: List[Effect]) extends AnyVal {
  /** Adds an effect to the end, merging it where it can.
    *
    * @param items the item data, which tells whether an item can fill a place
    */
  def add(effect: Effect, items: Item.ID => Item): EffectList =
    EffectList(addMarkingMerges(effect, items).map(_.effect))

  /** As `add`, saying which of the effects came from merging */
  def addMarkingMerges(effect: Effect, items: Item.ID => Item): List[EffectList.Entry] =
    EffectList.insert(unmerged, EffectList.Entry(effect, merged = false), items, keepLatestChoice = true)

  /** Merges the effects again, as after deleting, reordering or editing one. Only merges that leave
    * the step's result alone are made: keeping the later choice between an exact amount and the
    * most is for when an effect is added, and would otherwise replace effects nobody touched. */
  def reconciled(items: Item.ID => Item): EffectList =
    EffectList(reconciledMarkingMerges(items).map(_.effect))

  /** As `reconciled`, saying which of the effects came from merging */
  def reconciledMarkingMerges(items: Item.ID => Item): List[EffectList.Entry] =
    underlying.foldLeft(List.empty[EffectList.Entry])((entries, effect) =>
      EffectList.insert(entries, EffectList.Entry(effect, merged = false), items, keepLatestChoice = false)
    )

  def -(effect: Effect): EffectList =
    EffectList(underlying.filterNot(_ == effect))

  /** @param keepLatestChoice whether a later exact amount replaces an earlier most, or the most
    *                         an earlier exact amount, which changes what the step does. Tests turn
    *                         it off to check that every other merge leaves the step's result alone.
    */
  private[plan] def plus(effect: Effect, items: Item.ID => Item, keepLatestChoice: Boolean): EffectList =
    EffectList(EffectList.insert(unmerged, EffectList.Entry(effect, merged = false), items, keepLatestChoice).map(_.effect))

  private def unmerged: List[EffectList.Entry] =
    underlying.map(EffectList.Entry(_, merged = false))
}
