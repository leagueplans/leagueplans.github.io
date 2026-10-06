package com.leagueplans.ui.model.plan.merge

import com.leagueplans.common.model.Item
import com.leagueplans.ui.model.plan.{Effect, EffectList, ItemChange, ItemQuantity}
import com.leagueplans.ui.model.plan.Effect.{AddItem, DepositAll, MoveItem}
import com.leagueplans.ui.model.plan.merge.MergeRules.Merged
import com.leagueplans.ui.model.player.Player
import com.leagueplans.ui.model.player.item.ItemEffects

/** Keeps a step's effects merged, so that a step reads as what it does rather than every click
  * that made it. `MergeRules` says which pairs of effects make a single change, and an effect only
  * merges with an earlier one if it can move back next to it without changing what the step does,
  * which `EffectDependencies` decides.
  *
  * Whatever a merge leaves that comes to nothing at the step is dropped too: unequipping everything
  * just after equipping it, say, leaves a move of everything that's equipped, which is nothing if
  * nothing was equipped when the step started. That needs the player before the step, so nothing
  * is dropped without it.
  */
object StepEffects {
  /** Adds an effect to the end of a step's effects, merging it where it can. Between an exact
    * amount and the most, the later choice replaces the earlier.
    *
    * @param playerAtStart the player before the step's effects apply, if it's known
    * @param items the item data, which tells whether an item can fill a place
    */
  def add(effects: EffectList, effect: Effect, playerAtStart: Option[Player], items: Item.ID => Item): EffectList =
    withoutMergedNoOps(insert(unmerged(effects), Entry(effect, merged = false), items, keepLatestChoice = true), playerAtStart, items)

  /** Merges a step's effects again, as after deleting, reordering or editing one. Only merges that
    * leave the step's result alone are made: keeping the later choice between an exact amount and
    * the most is for when an effect is added, and would otherwise replace effects nobody touched.
    *
    * @param playerAtStart the player before the step's effects apply, if it's known
    */
  def reconcile(effects: EffectList, playerAtStart: Option[Player], items: Item.ID => Item): EffectList =
    withoutMergedNoOps(
      effects.underlying.foldLeft(List.empty[Entry])((entries, effect) =>
        insert(entries, Entry(effect, merged = false), items, keepLatestChoice = false)
      ),
      playerAtStart,
      items
    )

  /** As `add`, without dropping anything. Tests turn off keeping the later choice to check that
    * every other merge leaves the step's result alone. */
  private[merge] def merged(effects: EffectList, effect: Effect, items: Item.ID => Item, keepLatestChoice: Boolean): EffectList =
    EffectList(insert(unmerged(effects), Entry(effect, merged = false), items, keepLatestChoice).map(_.effect))

  /** An effect, and whether it came from merging others */
  private final case class Entry(effect: Effect, merged: Boolean)

  private def unmerged(effects: EffectList): List[Entry] =
    effects.underlying.map(Entry(_, merged = false))

  /** Adds an entry to the end of the others, merging it with the nearest earlier effect it can,
    * looking back only as far as the new effect could move without changing the step's result */
  private def insert(entries: List[Entry], entry: Entry, items: Item.ID => Item, keepLatestChoice: Boolean): List[Entry] = {
    val effects = entries.map(_.effect)
    def canReach(i: Int): Boolean =
      effects.drop(i + 1).forall(EffectDependencies.canMoveBefore(entry.effect, _))

    effects.zipWithIndex.reverse
      .takeWhile((_, i) => canReach(i))
      .collectFirst(Function.unlift((existing, i) => MergeRules.merge(existing, entry.effect, items, keepLatestChoice).map((_, i))))
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

  /** Drops the effects that came from merging and come to nothing where they apply */
  private def withoutMergedNoOps(entries: List[Entry], playerAtStart: Option[Player], items: Item.ID => Item): EffectList =
    playerAtStart match {
      case None =>
        EffectList(entries.map(_.effect))
      case Some(start) =>
        val (_, kept) = entries.foldLeft((start, List.empty[Effect])) { case ((player, kept), entry) =>
          val keep = !entry.merged || !comesToNothing(entry.effect, player, items)
          (applyIfItem(player, entry.effect, items), if (keep) kept :+ entry.effect else kept)
        }
        EffectList(kept)
    }

  /** Whether an effect that's worked out where it applies changes nothing */
  private def comesToNothing(effect: Effect, player: Player, items: Item.ID => Item): Boolean =
    effect match {
      case e @ AddItem(_, ItemChange.Fill | ItemChange.Empty, _, _) => ItemEffects.count(e, player, items) == 0
      case e @ MoveItem(_, ItemQuantity.Max, _, _, _, _) => ItemEffects.count(e, player, items) == 0
      case DepositAll(source) => ItemEffects.deposits(source, player, items).isEmpty
      case _ => false
    }

  private def applyIfItem(player: Player, effect: Effect, items: Item.ID => Item): Player =
    effect match {
      case e: (AddItem | MoveItem | DepositAll) => ItemEffects(player, e, items)
      case _ => player
    }
}
