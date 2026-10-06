package com.leagueplans.ui.dom.planning.drag

import com.leagueplans.ui.dom.planning.drag.DragSession.Dragged.{DraggedEffect, DraggedRequirement, DraggedStepContent}
import com.leagueplans.common.model.Item
import com.leagueplans.ui.model.plan.{EffectList, Requirement, Step}
import com.leagueplans.ui.model.player.Player
import com.leagueplans.ui.model.player.item.ItemEffects

/** Moves an effect or requirement dragged out of one step's details onto another step */
object StepContentTransfer {
  /** Removes the content from the source step and adds it to the end of the target step, merging
    * it with matching content there. [[DropRules]] decides which steps can take it.
    *
    * Both steps' effects are then merged with `StepEffects.reconcile`, as after an edit, rather
    * than `add`, so the moved effect doesn't replace "all" or "until full" choices already in the
    * target.
    *
    * Merging can leave an effect that does nothing, which can only be spotted from the player at
    * the start of its step. Drags start from the step details, so the source is the focused step,
    * whose starting player the page has; the target can be any step, whose starting player only
    * the projection worker knows. So only the source is cleaned up.
    *
    * @param sourcePlayerBefore the player at the start of the source step, if it's known
    *
    * @return nothing if the content isn't where the drag found it, since another tab could have
    *         changed the step during the drag
    */
  def apply(
    dragged: DraggedStepContent,
    source: Step,
    target: Step,
    items: Item.ID => Item,
    sourcePlayerBefore: Option[Player]
  ): Option[(source: Step, target: Step)] =
    dragged match {
      case DraggedEffect(_, index, effect) =>
        Option.when(source.directEffects.underlying.lift(index).contains(effect))((
          source = source.deepCopy(directEffects =
            ItemEffects.reconcileStep(EffectList(source.directEffects.underlying.patch(index, Nil, 1)), sourcePlayerBefore, items)
          ),
          target = target.deepCopy(directEffects = EffectList(target.directEffects.underlying :+ effect).reconciled(items))
        ))

      case DraggedRequirement(_, index, requirement) =>
        Option.when(source.requirements.lift(index).contains(requirement))((
          source = source.deepCopy(requirements = source.requirements.patch(index, Nil, 1)),
          target = target.deepCopy(requirements = Requirement.addTo(target.requirements, requirement))
        ))
    }
}
