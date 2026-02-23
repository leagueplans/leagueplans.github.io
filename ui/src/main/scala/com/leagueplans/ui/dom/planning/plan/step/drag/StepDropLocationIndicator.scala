package com.leagueplans.ui.dom.planning.plan.step.drag

import com.raquo.airstream.core.Signal
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, enrichSource, seqToModifier}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object StepDropLocationIndicator {
  private type BoundingRect = (width: Double, height: Double, top: Double, left: Double)

  def apply(
    locationSignal: Signal[StepDraggingStatus],
    parentElement: L.HtmlElement
  ): L.Modifier[L.HtmlElement] = {
    val maybeDimensions = Var[Option[BoundingRect]](None)

    List(
      locationSignal.distinct.map(getBoundingRect(_, parentElement)) --> maybeDimensions.writer,
      L.child.maybe <-- maybeDimensions.signal.splitOption((_, dimensions) =>
        L.div(
          L.cls(Styles.indicator),
          L.width <-- dimensions.getPixels(_.width),
          L.height <-- dimensions.getPixels(_.height),
          L.top <-- dimensions.getPixels(_.top),
          L.left <-- dimensions.getPixels(_.left),
        )
      )
    )
  }

  @js.native @JSImport("/styles/planning/plan/step/drag/dropLocationIndicator.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val indicator: String = js.native
  }

  extension (self: Signal[BoundingRect]) {
    private def getPixels(f: BoundingRect => Double): Signal[String] =
      self.map(boundingRect =>
        // Constructed manually to avoid loss of precision from casting to an int for L.style.px
        s"${f(boundingRect)}px"
      )
  }

  private def getBoundingRect(
    dragStatus: StepDraggingStatus,
    parentElement: L.HtmlElement
  ): Option[BoundingRect] =
    dragStatus match {
      case StepDraggingStatus.Dragging(Some(dropTarget)) =>
        Some(getBoundingRect(dropTarget, parentElement))
      case _ =>
        None
    }

  private def getBoundingRect(
    dropTarget: StepDraggingStatus.DropTarget,
    parentElement: L.HtmlElement
  ): BoundingRect = {
    val parentBounds = parentElement.ref.getBoundingClientRect()

    dropTarget.relativePosition match {
      case StepDraggingStatus.DropTarget.RelativePosition.Into =>
        (
          width = dropTarget.bounds.width,
          height = dropTarget.bounds.height,
          top = dropTarget.bounds.top - parentBounds.top + parentElement.ref.scrollTop,
          left = dropTarget.bounds.left - parentBounds.left + parentElement.ref.scrollLeft
        )
      case StepDraggingStatus.DropTarget.RelativePosition.Before =>
        (
          width = dropTarget.bounds.width,
          height = 0.0,
          top = dropTarget.bounds.top - parentBounds.top + parentElement.ref.scrollTop,
          left = dropTarget.bounds.left - parentBounds.left + parentElement.ref.scrollLeft
        )
      case StepDraggingStatus.DropTarget.RelativePosition.After =>
        (
          width = dropTarget.bounds.width,
          height = 0.0,
          top = dropTarget.bounds.bottom - parentBounds.top + parentElement.ref.scrollTop,
          left = dropTarget.bounds.left - parentBounds.left + parentElement.ref.scrollLeft
        )
    }
  }
}
