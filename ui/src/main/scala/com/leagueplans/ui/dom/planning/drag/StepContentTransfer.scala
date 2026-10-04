package com.leagueplans.ui.dom.planning.drag

import com.leagueplans.ui.dom.planning.drag.DragSession.Dragged.{DraggedEffect, DraggedRequirement, DraggedStepContent}
import com.leagueplans.ui.model.plan.{EffectList, Requirement, Step}

/** Moves an effect or requirement dragged out of one step's details onto another step */
object StepContentTransfer {
  /** The two steps after the move. The content is added to the end of the target's list, merged
    * with matching content if there is some. [[DropRules]] decides which steps it can move to.
    *
    * @return nothing if the content isn't where the drag found it, since another tab could have
    *         changed the step during the drag
    */
  def apply(dragged: DraggedStepContent, source: Step, target: Step): Option[(source: Step, target: Step)] =
    dragged match {
      case DraggedEffect(_, index, effect) =>
        Option.when(source.directEffects.underlying.lift(index).contains(effect))((
          source = source.deepCopy(directEffects = EffectList(source.directEffects.underlying.patch(index, Nil, 1))),
          target = target.deepCopy(directEffects = target.directEffects + effect)
        ))

      case DraggedRequirement(_, index, requirement) =>
        Option.when(source.requirements.lift(index).contains(requirement))((
          source = source.deepCopy(requirements = source.requirements.patch(index, Nil, 1)),
          target = target.deepCopy(requirements = Requirement.addTo(target.requirements, requirement))
        ))
    }
}
