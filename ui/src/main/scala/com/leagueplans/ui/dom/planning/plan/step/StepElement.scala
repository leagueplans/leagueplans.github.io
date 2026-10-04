package com.leagueplans.ui.dom.planning.plan.step

import com.leagueplans.ui.dom.planning.details.EditRequest
import com.leagueplans.ui.dom.planning.drag.DragSession
import com.leagueplans.ui.dom.planning.plan.step.drag.{PlanDropZone, StepDragSource, StepDraggingStatus}
import com.leagueplans.ui.dom.planning.plan.{CollapsedSteps, CompletedStep, FocusController, NewStepDraft, StepClipboard}
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.ui.projection.calculation.TimeKeeper
import com.leagueplans.uicommon.dom.collapse.{HeightMask, InvertibleAnimationController}
import com.leagueplans.uicommon.dom.{ContextMenu, Tooltip}
import com.leagueplans.uicommon.facades.floatingui.Placement
import com.leagueplans.uicommon.utils.laminar.EventProcessorOps.handledAs
import com.leagueplans.uicommon.utils.laminar.HtmlElementOps.trackHeight
import com.leagueplans.uicommon.utils.laminar.LaminarOps.onKey
import com.leagueplans.uicommon.wrappers.floatingui.FloatingConfig
import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, StringValueMapper, enrichSource, eventPropToProcessor, seqToModifier, textToTextNode}
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
    isFocused: Signal[Boolean],
    substepFocused: EventStream[Unit],
    focusController: FocusController,
    collapsedSteps: CollapsedSteps,
    completionController: CompletedStep.Controller,
    draggingStatus: Var[StepDraggingStatus],
    hasErrorsSignal: Signal[Boolean],
    editingEnabledSignal: Signal[Boolean],
    timeKeeper: TimeKeeper,
    tooltip: Tooltip,
    contextMenu: ContextMenu,
    stepClipboard: StepClipboard,
    dragSession: DragSession,
    editInDetails: EditRequest => Unit,
    newStepDraft: NewStepDraft
  ): (L.Div, Signal[Int]) = {
    val isCompleted = completionController.signalFor(stepID)
    val isHovering = Var(false)
    val isDraggable = Var(false)
    val isDraggingSignal = draggingStatus.signal.map(_ != StepDraggingStatus.NotDragging).distinct
    val animationController = InvertibleAnimationController(
      startOpen = !collapsedSteps.isCollapsed(stepID),
      animationDuration = 200.millis
    )
    val header = toHeader(
      stepID,
      step,
      substepsSignal,
      isDraggingSignal,
      isFocused,
      isCompleted,
      hasErrorsSignal,
      isHovering.signal,
      isDraggable,
      editRepetitions = () => {
        focusController.set(stepID)
        editInDetails(EditRequest.Repetitions)
      },
      animationController,
      timeKeeper,
      positionOffset,
      tooltip
    )
    val headerHeight = header.trackHeight()

    val element =
      L.div(
        L.cls(Styles.step),
        L.tabIndex(0),
        L.draggable <-- isDraggable,
        header,
        L.div(L.cls(Styles.substepsSidebar)),
        toSubsteps(substepsSignal, newStepDraft.rowIn(Some(stepID)), isDraggingSignal, animationController),
        tooltip.register(
          L.span(L.cls(Styles.tooltip), "Click to focus or unfocus"),
          FloatingConfig.basicAnchoredTooltip(anchor = header, Placement.left, includeArrow = true)
        ),
        toFocusListeners(stepID, isFocused, focusController),
        substepFocused --> (_ => animationController.open()),
        // A new step being typed in among the substeps must be visible
        newStepDraft.position.changes.filter(_.exists(_.parent.contains(stepID))) --> (_ => animationController.open()),
        animationController.statusSignal.changes.collect {
          case InvertibleAnimationController.Status.Open => false
          case InvertibleAnimationController.Status.Closed => true
        } --> (collapsed => collapsedSteps.set(stepID, collapsed)),
        toHoverListeners(isHovering),
        PlanDropZone.stepMarker(stepID),
        StepDragSource(
          stepID,
          hasSubsteps = substepsSignal.map(_.nonEmpty),
          draggingStatus.writer,
          header,
          closeSubsteps = animationController.close,
          dragSession,
          tooltip
        ),
        StepContextMenu(
          stepID,
          contextMenu,
          stepClipboard,
          completionController,
          editingEnabledSignal
        )
      )

    (element, toChildOffset(animationController, positionOffset, headerHeight))
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
    stepID: Step.ID,
    step: Signal[Step],
    substepsSignal: Signal[List[L.HtmlElement]],
    isDragging: Signal[Boolean],
    isFocused: Signal[Boolean],
    isCompleted: Signal[Boolean],
    hasErrors: Signal[Boolean],
    isHovering: Signal[Boolean],
    isDraggable: Var[Boolean],
    editRepetitions: () => Unit,
    animationController: InvertibleAnimationController,
    timeKeeper: TimeKeeper,
    positionOffset: Signal[Int],
    tooltip: Tooltip
  ): L.Div =
    StepHeader(
      stepID,
      step,
      substepsSignal.map(_.nonEmpty),
      isFocused,
      isCompleted,
      hasErrors,
      isDraggable.writer,
      editRepetitions,
      animationController,
      timeKeeper,
      tooltip
    ).amend(
      // Only the header, so that a step's state isn't mistaken for its substeps'
      L.cls <-- Signal.combine(isFocused, isCompleted, hasErrors, isHovering).map(StepBackground.from),
      PlanDropZone.headerMarker,
      L.cls <-- isDragging.splitBoolean(
        whenTrue = _ => Styles.headerWhileDragging,
        whenFalse = _ => Styles.headerWhileNotDragging
      ),
      L.top <-- positionOffset.map(offset => L.style.px(offset))
    )

  private def toSubsteps(
    substepsSignal: Signal[List[L.HtmlElement]],
    newStepRow: Signal[Option[(Int, L.Div)]],
    isDragging: Signal[Boolean],
    animationController: InvertibleAnimationController
  ): L.Div = {
    val substepItems = substepsSignal.split(identity)((child, _, _) => L.li(L.cls(Styles.substep), child))
    val list = L.ol(
      L.cls(Styles.substepList),
      L.children <-- Signal.combine(substepItems, newStepRow.map(_.map((index, row) => (index, L.li(row)))))
        .map(NewStepDraft.insertRow)
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
    isFocused: Signal[Boolean],
    focusController: FocusController,
  ): L.Modifier[L.HtmlElement] =
    List(
      L.onClick.handledAs(stepID) --> focusController.toggle,
      L.onKey(KeyValue.Enter).handledAs(stepID) --> focusController.toggle,
      L.inContext[L.HtmlElement](ctx =>
        // Deferred until the DOM has settled, so that a step inside a collapsed superstep can
        // take focus once the superstep has started to open. Both directions are deferred to
        // keep them in order.
        isFocused.changes.delay(ms = 0) --> {
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
