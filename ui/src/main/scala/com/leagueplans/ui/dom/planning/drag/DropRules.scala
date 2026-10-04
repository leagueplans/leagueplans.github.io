package com.leagueplans.ui.dom.planning.drag

import com.leagueplans.ui.dom.planning.drag.DragSession.Dragged
import com.leagueplans.ui.dom.planning.drag.DragSession.Dragged.{DraggedStep, DraggedStepContent}
import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.Step

/** What can be dropped on which step. Checked as the pointer moves over the plan, so that the
  * browser shows whether a drop is possible, and again when the drop is applied, since the plan
  * could have changed in between. */
object DropRules {
  def canDrop(dragged: Dragged, onto: Step.ID, forest: Forest[Step.ID, Step]): Boolean =
    dragged match {
      // A step can't be moved into itself or its own substeps. Walking up from the target takes
      // one lookup per level of nesting, however big the plan is.
      case DraggedStep(id) => id != onto && !forest.ancestors(onto).contains(id)
      // Dropping an effect or requirement back on its own step would do nothing
      case content: DraggedStepContent => content.from != onto
    }
}
