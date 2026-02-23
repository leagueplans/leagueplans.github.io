package com.leagueplans.ui.dom.planning.plan.step.drag

import org.scalajs.dom.DOMRect

object StepDraggingStatus {
  object DropTarget {
    enum RelativePosition {
      case Before, Into, After
    }
  }

  final case class DropTarget(bounds: DOMRect, relativePosition: DropTarget.RelativePosition)
}

enum StepDraggingStatus {
  case NotDragging
  case Dragging(currentDropTarget: Option[StepDraggingStatus.DropTarget])
}
