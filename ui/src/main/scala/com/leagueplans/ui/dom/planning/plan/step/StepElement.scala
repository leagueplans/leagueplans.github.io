package com.leagueplans.ui.dom.planning.plan.step

import com.leagueplans.ui.dom.common.collapse.{HeightMask, InvertibleAnimationController}
import com.leagueplans.ui.dom.common.{ContextMenu, Tooltip}
import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.dom.planning.plan.step.drag.{StepDragListeners, StepDraggingStatus}
import com.leagueplans.ui.dom.planning.plan.{CompletedStep, FocusedStep}
import com.leagueplans.ui.facades.floatingui.Placement
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.ui.utils.laminar.EventProcessorOps.handledAs
import com.leagueplans.ui.utils.laminar.HtmlElementOps.trackHeight
import com.leagueplans.ui.utils.laminar.LaminarOps.onKey
import com.leagueplans.ui.wrappers.Clipboard
import com.leagueplans.ui.wrappers.floatingui.FloatingConfig
import com.raquo.airstream.core.Signal
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, StringValueMapper, enrichSource, eventPropToProcessor, seqToModifier, textToTextNode}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.{KeyValue, document}

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
    draggingStatus: Var[StepDraggingStatus],
    hasErrorsSignal: Signal[Boolean],
    editingEnabledSignal: Signal[Boolean],
    tooltip: Tooltip,
    contextMenuController: ContextMenu.Controller,
    clipboard: Clipboard[Step]
  ): (L.Div, Signal[Int]) = {
    val isFocused = focusController.signalFor(stepID)
    val isCompleted = completionController.signalFor(stepID)
    val isHovering = Var(false)
    val isDraggable = Var(false)
    val isDraggingSignal = draggingStatus.signal.map(_ != StepDraggingStatus.NotDragging).distinct
    val animationController = InvertibleAnimationController(
      startOpen = true,
      animationDuration = 200.millis
    )
    val header = toHeader(
      step,
      substepsSignal,
      isDraggingSignal,
      isFocused,
      isDraggable,
      animationController,
      positionOffset,
      tooltip
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
        toSubsteps(substepsSignal, isDraggingSignal, animationController),
        tooltip.register(
          L.span(L.cls(Styles.tooltip), "Click to toggle focus"),
          FloatingConfig.basicAnchoredTooltip(anchor = header, Placement.left, includeArrow = true)
        ),
        toFocusListeners(stepID, focusController),
        toHoverListeners(isHovering),
        StepDragListeners(
          stepID,
          hasSubsteps = substepsSignal.map(_.nonEmpty),
          draggingStatus.writer,
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
    val tooltip: String = js.native
  }

  private def toHeader(
    step: Signal[Step],
    substepsSignal: Signal[List[L.HtmlElement]],
    isDragging: Signal[Boolean],
    isFocused: Signal[Boolean],
    isDraggable: Var[Boolean],
    animationController: InvertibleAnimationController,
    positionOffset: Signal[Int],
    tooltip: Tooltip
  ): L.Div =
    StepHeader(
      step,
      substepsSignal.map(_.nonEmpty),
      isFocused,
      isDraggable.writer,
      animationController,
      tooltip
    ).amend(
      L.cls <-- isDragging.splitBoolean(
        whenTrue = _ => Styles.headerWhileDragging,
        whenFalse = _ => Styles.headerWhileNotDragging
      ),
      L.top <-- positionOffset.map(offset => L.style.px(offset))
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
      L.onKey(KeyValue.Enter).handledAs(stepID) --> focusController.toggle,
      L.inContext[L.HtmlElement](ctx =>
        focusController.signalFor(stepID).changes --> {
          case true => ctx.ref.focus()
          case false => ctx.ref.blur()
        }
      )
    )

  private def toHoverListeners(isHovering: Var[Boolean]): L.Modifier[L.HtmlElement] =
    List(
      L.onMouseOver.handledAs(true) --> isHovering,
      L.inContext[L.HtmlElement](ctx =>
        L.onMouseOut.handledAs(
          document.activeElement == ctx.ref && document.hasFocus()
        ) --> isHovering
      ),
      L.onMouseOut.handledAs(false) --> isHovering,
      L.onFocus.handledAs(true) --> isHovering,
      L.onBlur.handledAs(false) --> isHovering
    )

  private def toChildOffset(
    animationController: InvertibleAnimationController,
    parentOffsetSignal: Signal[Int],
    headerHeightSignal: Signal[Int],
  ): Signal[Int] =
    Signal.combine(
      animationController.statusSignal,
      parentOffsetSignal,
      headerHeightSignal
    ).map {
      case (InvertibleAnimationController.Status.Open, offset, headerHeight) =>
        offset + headerHeight
      case _ =>
        0
    }
}
