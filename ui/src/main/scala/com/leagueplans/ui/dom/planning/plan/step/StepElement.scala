package com.leagueplans.ui.dom.planning.plan.step

import com.leagueplans.ui.dom.common.collapse.{CollapseButton, HeightMask, InvertibleAnimationController}
import com.leagueplans.ui.dom.common.{ContextMenu, Tooltip}
import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.dom.planning.plan.{CompletedStep, FocusedStep}
import com.leagueplans.ui.facades.animation.{FillMode, KeyframeAnimationOptions}
import com.leagueplans.ui.facades.fontawesome.freesolid.FreeSolid
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.ui.utils.laminar.EventProcessorOps.handledAs
import com.leagueplans.ui.utils.laminar.FontAwesome
import com.leagueplans.ui.utils.laminar.LaminarOps.onKey
import com.leagueplans.ui.wrappers.Clipboard
import com.leagueplans.ui.wrappers.animation.{Animation, KeyframeProperty}
import com.raquo.airstream.core.Signal
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, StringValueMapper, eventPropToProcessor, seqToModifier, textToTextNode}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.KeyCode

import scala.concurrent.duration.{Duration, DurationInt}
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

//TODO Maybe you can get the sticky description stuff working through also returning the
//     header from the apply method
// steps:
//   position: relative
// headers:
//   background: inherit
//   top: dynamic
//   position: sticky
object StepElement {
  def apply(
    stepID: Step.ID,
    step: Signal[Step],
    substepsSignal: Signal[List[L.HtmlElement]],
    forester: Forester[Step.ID, Step],
    focusController: FocusedStep.Controller,
    completionController: CompletedStep.Controller,
    hasErrorsSignal: Signal[Boolean],
    editingEnabledSignal: Signal[Boolean],
    contextMenuController: ContextMenu.Controller,
    clipboard: Clipboard[Step]
  ): L.Div = {
    val isFocused = focusController.signalFor(stepID)
    val isCompleted = completionController.signalFor(stepID)
    val isHovering = Var(false)
    val isDraggable = Var(false)
    val animationController = InvertibleAnimationController(
      startOpen = true,
      animationDuration = 200.millis
    )
    val header = StepHeader(step, isFocused, isDraggable.writer).amend(L.cls(Styles.header))

    L.div(
      L.cls(Styles.step),
      L.cls <-- Signal.combine(isFocused, isCompleted, hasErrorsSignal, isHovering).map(StepBackground.from),
      L.tabIndex(0),
      L.draggable <-- isDraggable,
      L.child <-- toSubstepsToggle(animationController, substepsSignal),
      header,
      toSubsteps(substepsSignal, animationController),
      Tooltip(L.span("Click to toggle focus")),
      toFocusListeners(stepID, focusController),
      toHoverListeners(isHovering),
      StepContextMenu(
        stepID,
        step,
        forester,
        contextMenuController,
        clipboard,
        completionController,
        editingEnabledSignal,
      ),
      StepDragListeners(
        stepID,
        hasSubsteps = substepsSignal.map(_.nonEmpty),
        header,
        closeSubsteps = animationController.close,
        forester
      )
    )
  }

  @js.native @JSImport("/styles/planning/plan/step/step.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val step: String = js.native
    val substepsToggle: String = js.native
    val substepsToggleIcon: String = js.native
    val substepsSidebar: String = js.native
    val header: String = js.native
    val substeps: String = js.native
    val substepList: String = js.native
    val substep: String = js.native
  }

  private def toSubsteps(
    substepsSignal: Signal[List[L.HtmlElement]],
    animationController: InvertibleAnimationController
  ): L.Div = {
    val list = L.ol(
      L.cls(Styles.substepList),
      L.children <-- substepsSignal.split(identity)((child, _, _) =>
        L.li(L.cls(Styles.substep), child)
      )
    )

    HeightMask(list, animationController).amend(L.cls(Styles.substeps))
  }

  private def toSubstepsToggle(
    animationController: InvertibleAnimationController,
    substepsSignal: Signal[List[L.HtmlElement]]
  ): Signal[L.Node] =
    // We choose the icons orientation based on whether the animation controller is
    // currently open, so we need to make sure that we only create the icon at the
    // point where we go to render.
    substepsSignal.map(_.isEmpty).distinct.map {
      case true => L.emptyNode
      case false =>
        val icon = FontAwesome.icon(FreeSolid.faCaretRight).amend(
          L.svg.cls(Styles.substepsToggleIcon),
          L.svg.transform.maybe(Option.when(animationController.isOpen)("rotate(90)")),
          animationController(
            toOpen = rotate(_, targetRotation = 90),
            toClose = rotate(_, targetRotation = 0)
          )
        )

        CollapseButton(
          icon,
          animationController,
          tooltip = "Toggle substep visibility",
          screenReaderDescription = "toggle substep visibility"
        ).amend(
          L.cls(Styles.substepsToggle),
          L.div(L.cls(Styles.substepsSidebar))
        )
    }

  private def rotate(animationDuration: Duration, targetRotation: Double): Animation =
    Animation(
      new KeyframeAnimationOptions {
        duration = animationDuration.toMillis.toDouble
        fill = FillMode.forwards
      },
      List(KeyframeProperty.transform(s"rotate(${targetRotation}deg)"))
    )

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
}
