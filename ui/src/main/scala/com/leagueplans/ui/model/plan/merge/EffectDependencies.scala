package com.leagueplans.ui.model.plan.merge

import com.leagueplans.common.model.Item
import com.leagueplans.ui.model.plan.{Effect, ItemChange, ItemQuantity}
import com.leagueplans.ui.model.plan.Effect.*
import com.leagueplans.ui.model.player.item.Depository
import com.leagueplans.ui.model.player.item.Depository.Kind

/** What an effect depends on and changes, so that a step can tell when two of its effects can
  * swap places without changing what the step does. An effect can only merge with an earlier
  * one if it can move back next to it.
  */
private[merge] object EffectDependencies {
  enum Resource {
    /** The items in a stack. `None` stands for any item, or either noted state. */
    case Stack(item: Option[Item.ID], noted: Option[Boolean], place: Depository.Kind)
    /** The free space in the inventory or an equipment slot. Space in the bank isn't tracked:
      * it's rarely short, and tracking it would stop most deposits and withdrawals merging. */
    case Space(place: Depository.Kind)
  }

  /** @param reads what the effect's result depends on, for an effect that's worked out where it
    *              applies, such as a move of everything held
    * @param supplies what the effect adds to
    * @param consumes what the effect takes from
    */
  final case class Access(reads: Set[Resource], supplies: Set[Resource], consumes: Set[Resource]) {
    def writes: Set[Resource] = supplies ++ consumes
  }

  private val none = Access(Set.empty, Set.empty, Set.empty)

  /** Whether the later effect can move before the earlier one. Neither can depend on what the
    * other changes, and the later one can't take what the earlier one supplied, which it may
    * have needed. Taking from a stack or a place's space before it's added to could leave too
    * little, or overfill the place.
    */
  def canMoveBefore(later: Effect, earlier: Effect): Boolean = {
    val (l, e) = (access(later), access(earlier))
    !overlaps(e.writes, l.reads) && !overlaps(l.writes, e.reads) && !overlaps(e.supplies, l.consumes)
  }

  def access(effect: Effect): Access =
    effect match {
      case AddItem(item, change, place, noted) =>
        val stack = Resource.Stack(Some(item), Some(noted), place)
        change match {
          case ItemChange.By(n) if n < 0 => Access(Set.empty, space(place), Set(stack))
          case ItemChange.By(_) => Access(Set.empty, Set(stack), space(place))
          case ItemChange.Fill => Access(Set(stack) ++ space(place), Set(stack), space(place))
          case ItemChange.Empty => Access(Set(stack), space(place), Set(stack))
        }

      case MoveItem(item, quantity, source, notedInSource, target, noteInTarget) =>
        val from = Resource.Stack(Some(item), Some(notedInSource), source)
        val to = Resource.Stack(Some(item), Some(noteInTarget), target)
        val reads = quantity match {
          case ItemQuantity.Exact(_) => Set.empty
          // Everything held, and a withdrawal stops at what fits
          case ItemQuantity.Max => Set(from) ++ Option.when(target == Kind.Inventory)(to) ++ space(target)
        }
        Access(reads, Set(to) ++ space(source), Set(from) ++ space(target))

      case DepositAll(source) =>
        val held = source.places.map(Resource.Stack(None, None, _)).toSet
        Access(held, source.places.flatMap(space).toSet + Resource.Stack(None, Some(false), Kind.Bank), held)

      case _: (GainExp | UnlockSkill | CompleteQuest | CompleteDiaryTask | CompleteLeagueTask | CompleteGridTile) =>
        none
    }

  private def space(place: Depository.Kind): Set[Resource] =
    if (place == Kind.Bank) Set.empty else Set(Resource.Space(place))

  private def overlaps(a: Set[Resource], b: Set[Resource]): Boolean =
    a.exists(x => b.exists(overlap(x, _)))

  private def overlap(a: Resource, b: Resource): Boolean =
    (a, b) match {
      case (Resource.Stack(itemA, notedA, placeA), Resource.Stack(itemB, notedB, placeB)) =>
        placeA == placeB && matches(itemA, itemB) && matches(notedA, notedB)
      case (Resource.Space(placeA), Resource.Space(placeB)) =>
        placeA == placeB
      case _ =>
        false
    }

  private def matches[T](a: Option[T], b: Option[T]): Boolean =
    a.isEmpty || b.isEmpty || a == b
}
