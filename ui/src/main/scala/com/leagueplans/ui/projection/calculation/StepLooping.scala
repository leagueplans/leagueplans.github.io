package com.leagueplans.ui.projection.calculation

import com.leagueplans.ui.model.plan.{Effect, Repetitions, Step}
import com.leagueplans.ui.model.player.item.ItemEffects
import com.leagueplans.ui.model.player.{Cache, Player}
import com.leagueplans.ui.projection.calculation.loops.{LoopContext, Shortfalls}

/** Plays steps that repeat until a condition is met, for folds whose accumulator holds a player */
private[calculation] object StepLooping {
  def apply[Acc](resolver: EffectResolver, cache: Cache, player: Acc => Player): ForestFolder.Looping[Step, Acc] =
    new ForestFolder.Looping[Step, Acc] {
      def repetitions(step: Step): Repetitions =
        step.repetitions

      def context(acc: Acc, shortfalls: Shortfalls): LoopContext =
        LoopContext(player(acc), cache.items, shortfalls)

      def shortfalls(acc: Acc, step: Step, reps: Int): Shortfalls = {
        val effects = List.fill(reps)(step.directEffects.underlying).flatten
        effects.foldLeft((player(acc), Shortfalls.none)) { case ((current, found), effect) =>
          val missing = effect match {
            case change: (Effect.AddItem | Effect.MoveItem) => ItemEffects.shortfall(change, current).toMap
            case _ => Shortfalls.none
          }
          (resolver.resolve(current, effect), Shortfalls.combine(found, missing))
        }._2
      }
    }
}
