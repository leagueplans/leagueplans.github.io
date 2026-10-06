package com.leagueplans.ui.model.plan

import com.leagueplans.codec.decoding.CollectionDecoder
import com.leagueplans.codec.encoding.CollectionEncoder
import com.leagueplans.ui.model.plan.Effect.*
import com.leagueplans.ui.model.plan.ItemQuantity.{Exact, Max}
import com.leagueplans.ui.model.player.item.Depository
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

  private enum Merged {
    /** The later effect stands in for both, so it stays where it was added */
    case Later
    /** The two become one effect where the earlier was, or nothing if they cancel out */
    case Into(effect: Option[Effect])
  }

  /** Merges an effect with an earlier one that it sits next to, or None if they stay apart */
  private def merge(earlier: Effect, later: Effect, keepLatestChoice: Boolean): Option[Merged] =
    (earlier, later) match {
      case (e: GainExp, l: GainExp) if e.skill == l.skill =>
        Some(Merged.Into(Some(e.copy(baseExp = e.baseExp + l.baseExp)).filter(_.baseExp != Exp(0))))

      case (e: AddItem, l: AddItem) if e.item == l.item && e.target == l.target && e.note == l.note =>
        (e.change, l.change) match {
          case (ItemChange.By(a), ItemChange.By(b)) =>
            Some(Merged.Into(Option.when(a + b != 0)(e.copy(change = ItemChange.By(a + b)))))
          // Emptying a place leaves none, whatever it held before
          case (_, ItemChange.Empty) =>
            Some(Merged.Later)
          case (ItemChange.Fill, ItemChange.Fill) =>
            Some(Merged.Later)
          // Filling depends on whether the item stacks, which a step's effects can't tell, so it
          // only takes an exact addition's place as the later choice
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
  def +(effect: Effect): EffectList =
    plus(effect, keepLatestChoice = true)

  def -(effect: Effect): EffectList =
    EffectList(underlying.filterNot(_ == effect))

  /** @param keepLatestChoice whether a later exact amount replaces an earlier most, or the most
    *                         an earlier exact amount, which changes what the step does. Tests turn
    *                         it off to check that every other merge leaves the step's result alone.
    */
  private[plan] def plus(effect: Effect, keepLatestChoice: Boolean): EffectList = {
    // The nearest earlier effect that the new one can merge with, looking back only as far as the
    // new effect could move
    underlying.zipWithIndex.reverse
      .takeWhile((_, i) => canReach(effect, i))
      .collectFirst(Function.unlift((existing, i) => EffectList.merge(existing, effect, keepLatestChoice).map((_, i))))
      match {
        case None =>
          EffectList(underlying :+ effect)
        // The new effect stays at the end if the earlier one could move forward to meet it. It may
        // have freed space that what came between needed, say.
        case Some((EffectList.Merged.Later, i)) if underlying.drop(i + 1).forall(EffectDependencies.canMoveBefore(_, underlying(i))) =>
          // The new effect may stand in for more of the earlier effects
          EffectList(underlying.patch(i, Nil, 1)).plus(effect, keepLatestChoice)
        case Some((EffectList.Merged.Later, i)) =>
          val before = EffectList(underlying.take(i))
          EffectList(before.plus(effect, keepLatestChoice).underlying ++ underlying.drop(i + 1))
        case Some((EffectList.Merged.Into(merged), i)) =>
          // What's merged takes the earlier effect's place, where it may merge again
          val before = EffectList(underlying.take(i))
          val after = underlying.drop(i + 1)
          EffectList(merged.fold(before)(before.plus(_, keepLatestChoice)).underlying ++ after)
      }
  }

  /** Whether an effect added at the end can move back to just after the effect at this index */
  private def canReach(effect: Effect, i: Int): Boolean =
    underlying.drop(i + 1).forall(EffectDependencies.canMoveBefore(effect, _))
}
