package com.leagueplans.ui.dom.plan

import com.leagueplans.ui.dom.common.collapse.{CollapseButton, HeightMask, InvertibleAnimationController}
import com.leagueplans.ui.dom.common.{ContextMenu, Tooltip}
import com.leagueplans.ui.dom.forest.Forester
import com.leagueplans.ui.facades.animation.{FillMode, KeyframeAnimationOptions}
import com.leagueplans.ui.facades.fontawesome.freesolid.FreeSolid
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.ui.utils.laminar.EventProcessorOps.handledAs
import com.leagueplans.ui.utils.laminar.FontAwesome
import com.leagueplans.ui.utils.laminar.LaminarOps.onKey
import com.leagueplans.ui.wrappers.Clipboard
import com.leagueplans.ui.wrappers.animation.{Animation, KeyframeProperty}
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, StringValueMapper, eventPropToProcessor, seqToModifier, textToTextNode}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.KeyCode

import scala.concurrent.duration.{Duration, DurationInt}
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object StepElement {
  def apply(
    stepID: Step.ID,
    step: Signal[Step],
    substepsSignal: Signal[List[L.HtmlElement]],
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
    val isDraggable = Var(false)
    val animationController = InvertibleAnimationController(
      startOpen = true,
      animationDuration = 200.millis
    )
    val header = StepHeader(step, isFocused, isDraggable.writer).amend(L.cls(Styles.header))

    L.div(
      L.cls(Styles.step),
      L.cls <-- Signal.combine(isFocused, isCompleteSignal, hasErrorsSignal, isHovering).map(StepBackground.from),
      L.tabIndex(0),
      L.draggable <-- isDraggable,
      L.child <-- toSubstepsToggle(animationController, substepsSignal),
      header,
      toSubsteps(substepsSignal, animationController),
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
      ),
      StepDragListeners(
        stepID,
        hasSubsteps = substepsSignal.map(_.nonEmpty),
        header,
        closeSubsteps = animationController.close,
        stepUpdater
      )
    )
  }

  @js.native @JSImport("/styles/plan/step.module.css", JSImport.Default)
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
  ): Signal[L.Node] = {
    val icon = FontAwesome.icon(FreeSolid.faCaretRight).amend(
      L.svg.cls(Styles.substepsToggleIcon),
      L.svg.transform.maybe(Option.when(animationController.isOpen)("rotate(90)")),
      animationController(
        toOpen = rotate(_, targetRotation = 90),
        toClose = rotate(_, targetRotation = 0)
      )
    )
    
    val button =
      CollapseButton(
        icon,
        animationController,
        tooltip = "Toggle substep visibility",
        screenReaderDescription = "toggle substep visibility"
      ).amend(
        L.cls(Styles.substepsToggle),
        L.div(L.cls(Styles.substepsSidebar))
      )

    substepsSignal.map(_.isEmpty).splitBoolean(
      whenTrue = _ => L.emptyNode,
      whenFalse = _ => button
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
