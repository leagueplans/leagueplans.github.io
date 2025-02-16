package com.leagueplans.ui.dom.plan

import com.leagueplans.ui.dom.common.collapse.{CollapseButton, HeightMask, InvertibleAnimationController}
import com.leagueplans.ui.dom.common.{ContextMenu, Tooltip}
import com.leagueplans.ui.dom.forest.Forester
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.ui.utils.laminar.LaminarOps.{handledAs, onKey}
import com.leagueplans.ui.wrappers.Clipboard
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, StringValueMapper, eventPropToProcessor, seqToModifier, textToTextNode}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.KeyCode
import org.scalajs.dom.html.Paragraph

import scala.concurrent.duration.DurationInt
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object StepElement {
  private val startOpen = true
  private val animationDuration = 200.millis

  def apply(
    stepID: Step.ID,
    step: Signal[Step],
    subStepsSignal: Signal[List[L.HtmlElement]],
    isFocused: Signal[Boolean],
    isCompleteSignal: Signal[Boolean],
    hasErrorsSignal: Signal[Boolean],
    editingEnabledSignal: Signal[Boolean],
    contextMenuController: ContextMenu.Controller,
    clipboard: Clipboard[Step],
    stepUpdater: Observer[Forester[Step.ID, Step] => Unit],
    completionStatusObserver: Observer[Boolean],
    focusObserver: Observer[Step.ID]
  ): L.Div = {
    val isHovering = Var(false)
    val (subSteps, subStepsMaskController) = toSubSteps(subStepsSignal)

    L.div(
      L.cls(Styles.step),
      L.cls <-- Signal.combine(isFocused, isCompleteSignal, hasErrorsSignal, isHovering).map(StepBackground.from),
      L.tabIndex(0),
      L.child <-- toSubStepsToggle(subStepsMaskController, subStepsSignal),
      toDescription(step),
      subSteps,
      Tooltip(L.span("Click to toggle focus")),
      toFocusListeners(stepID, focusObserver),
      toHoverListeners(isHovering),
      StepContextMenu(
        stepID,
        step,
        contextMenuController,
        clipboard,
        isCompleteSignal,
        editingEnabledSignal,
        stepUpdater,
        completionStatusObserver
      )
    )
  }

  @js.native @JSImport("/styles/plan/step.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val step: String = js.native
    val subStepsToggle: String = js.native
    val sideBar: String = js.native
    val description: String = js.native
    val subSteps: String = js.native
    val subStepList: String = js.native
    val subStep: String = js.native
  }

  private def toSubSteps(subStepsSignal: Signal[List[L.HtmlElement]]): (L.Div, InvertibleAnimationController) = {
    val list = L.ol(
      L.cls(Styles.subStepList),
      L.children <-- subStepsSignal.split(identity)((child, _, _) =>
        L.li(L.cls(Styles.subStep), child)
      )
    )

    val (mask, controller) = HeightMask(list, startOpen, animationDuration)
    (mask.amend(L.cls(Styles.subSteps)), controller)
  }

  private def toSubStepsToggle(
    subStepsMaskController: InvertibleAnimationController,
    subStepsSignal: Signal[List[L.HtmlElement]]
  ): Signal[L.Node] =
    subStepsSignal.map(_.isEmpty).splitBoolean(
      whenTrue = _ => L.emptyNode,
      whenFalse = _ => CollapseButton(
        startOpen,
        animationDuration,
        subStepsMaskController,
        tooltip = "Toggle sub-step visibility",
        screenReaderDescription = "toggle sub-step visibility"
      ).amend(
        L.cls(Styles.subStepsToggle),
        L.div(L.cls(Styles.sideBar))
      )
    )

  private def toDescription(stepSignal: Signal[Step]): ReactiveHtmlElement[Paragraph] =
    L.p(
      L.cls(Styles.description),
      L.text <-- stepSignal.map(_.description)
    )

  private def toFocusListeners(
    stepID: Step.ID,
    focusObserver: Observer[Step.ID]
  ): L.Modifier[L.HtmlElement] =
    List(
      L.onClick.handledAs(stepID) --> focusObserver,
      L.onKey(KeyCode.Enter).handledAs(stepID) --> focusObserver
    )

  private def toHoverListeners(isHovering: Var[Boolean]): L.Modifier[L.HtmlElement] = {
    List(
      L.onMouseOver.handledAs(true) --> isHovering,
      L.onMouseOut.handledAs(false) --> isHovering
    )
  }
}
