package com.leagueplans.ui.dom.planning.plan.step

import com.leagueplans.ui.dom.common.collapse.{HeightMask, InvertibleAnimationController}
import com.leagueplans.ui.dom.common.{ContextMenu, Tooltip}
import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.dom.planning.plan.{CompletedStep, FocusedStep}
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.ui.utils.laminar.EventProcessorOps.handledAs
import com.leagueplans.ui.utils.laminar.HtmlElementOps.trackHeight
import com.leagueplans.ui.utils.laminar.LaminarOps.onKey
import com.leagueplans.ui.wrappers.Clipboard
import com.raquo.airstream.core.Signal
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, StringValueMapper, eventPropToProcessor, seqToModifier, textToTextNode}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.KeyCode

import scala.concurrent.duration.DurationInt
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object StepElement {
  def apply(
    stepID: Step.ID,
    step: Signal[Step],
    positionOffset: Signal[Int],
    substepsSignal: Signal[List[L.HtmlElement]],
    forester: Forester[Step.ID, Step],
    focusController: FocusedStep.Controller,
    completionController: CompletedStep.Controller,
    isDragging: Var[Boolean],
    hasErrorsSignal: Signal[Boolean],
    editingEnabledSignal: Signal[Boolean],
    contextMenuController: ContextMenu.Controller,
    clipboard: Clipboard[Step]
  ): (L.Div, Signal[Int]) = {
    val isFocused = focusController.signalFor(stepID)
    val isCompleted = completionController.signalFor(stepID)
    val isHovering = Var(false)
    val isDraggable = Var(false)
    val animationController = InvertibleAnimationController(
      startOpen = true,
      animationDuration = 200.millis
    )
    val header = toHeader(
      step,
      substepsSignal,
      isDragging.signal,
      isFocused,
      isDraggable,
      animationController,
      positionOffset
    )
    val headerHeight = header.trackHeight()

    val element =
      L.div(
        L.cls(Styles.step),
        L.cls <-- Signal.combine(isFocused, isCompleted, hasErrorsSignal, isHovering).map(StepBackground.from),
        L.tabIndex(0),
        L.draggable <-- isDraggable,
        header,
        L.div(L.cls(Styles.substepsSidebar)),
        toSubsteps(substepsSignal, isDragging.signal, animationController),
        Tooltip(L.span("Click to toggle focus")),
        toFocusListeners(stepID, focusController),
        toHoverListeners(isHovering),
        StepDragListeners(
          stepID,
          hasSubsteps = substepsSignal.map(_.nonEmpty),
          isDragging.writer,
          header,
          closeSubsteps = animationController.close,
          forester
        ),
        StepContextMenu(
          stepID,
          step,
          forester,
          contextMenuController,
          clipboard,
          completionController,
          editingEnabledSignal,
        )
      )

    (element, toChildOffset(animationController, headerHeight, positionOffset))
  }

  @js.native @JSImport("/styles/planning/plan/step/step.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val step: String = js.native
    val substepsSidebar: String = js.native
    val headerWhileNotDragging: String = js.native
    val headerWhileDragging: String = js.native
    val substepsWhileNotDragging: String = js.native
    val substepsWhileDragging: String = js.native
    val substepList: String = js.native
    val substep: String = js.native
  }

  private def toHeader(
    step: Signal[Step],
    substepsSignal: Signal[List[L.HtmlElement]],
    isDragging: Signal[Boolean],
    isFocused: Signal[Boolean],
    isDraggable: Var[Boolean],
    animationController: InvertibleAnimationController,
    positionOffset: Signal[Int]
  ): L.Div =
    StepHeader(
      step,
      substepsSignal.map(_.nonEmpty),
      isFocused,
      isDraggable.writer,
      animationController
    ).amend(
      L.cls <-- isDragging.splitBoolean(
        whenTrue = _ => Styles.headerWhileDragging,
        whenFalse = _ => Styles.headerWhileNotDragging
      ),
      L.top <-- positionOffset.map(offset => s"${offset}px")
    )

  private def toSubsteps(
    substepsSignal: Signal[List[L.HtmlElement]],
    isDragging: Signal[Boolean],
    animationController: InvertibleAnimationController
  ): L.Div = {
    val list = L.ol(
      L.cls(Styles.substepList),
      L.children <-- substepsSignal.split(identity)((child, _, _) =>
        L.li(L.cls(Styles.substep), child)
      )
    )

    HeightMask(list, animationController).amend(
      L.cls <-- isDragging.splitBoolean(
        whenTrue = _ => Styles.substepsWhileDragging,
        whenFalse = _ => Styles.substepsWhileNotDragging
      )
    )
  }

  private def toFocusListeners(
    stepID: Step.ID,
    focusController: FocusedStep.Controller,
  ): L.Modifier[L.HtmlElement] =
    List(
      L.onClick.handledAs(stepID) --> focusController.toggle,
      L.onKey(KeyCode.Enter).handledAs(stepID) --> focusController.toggle
    )

  private def toHoverListeners(isHovering: Var[Boolean]): L.Modifier[L.HtmlElement] =
    List(
      L.onMouseOver.handledAs(true) --> isHovering,
      L.onMouseOut.handledAs(false) --> isHovering
    )

  private def toChildOffset(
    animationController: InvertibleAnimationController,
    headerHeightSignal: Signal[Int],
    parentOffsetSignal: Signal[Int]
  ): Signal[Int] =
    Signal.combine(
      animationController.statusSignal,
      headerHeightSignal,
      parentOffsetSignal
    ).map {
      case (InvertibleAnimationController.Status.Open, headerHeight, parentOffset) =>
        // Not sure why, but without the -1, there's sometimes a gap between the elements
        headerHeight + parentOffset - 1
      case _ =>
        0
    }
}
