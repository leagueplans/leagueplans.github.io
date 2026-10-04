package com.leagueplans.ui.dom.planning.plan

import com.leagueplans.ui.dom.planning.plan.history.UndoController
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.uicommon.dom.{Button, IconButtonModifiers, Modal, Tooltip}
import com.leagueplans.uicommon.facades.floatingui.Placement
import com.leagueplans.uicommon.facades.fontawesome.freesolid.FreeSolid
import com.leagueplans.uicommon.utils.laminar.EventProcessorOps.{handled, handledWith}
import com.leagueplans.uicommon.utils.laminar.FontAwesome
import com.leagueplans.uicommon.wrappers.floatingui.FloatingConfig
import com.raquo.airstream.core.{EventStream, Observer, Signal}
import com.raquo.laminar.api.{L, StringSeqValueMapper, seqToModifier, textToTextNode}
import com.raquo.laminar.codecs.StringAsIsCodec

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object PlanHeader {
  def apply(
    planName: String,
    focus: Signal[Option[Step.ID]],
    tooltip: Tooltip,
    modal: Modal,
    newStepDraft: NewStepDraft,
    stepDeleter: StepDeleter,
    undoController: UndoController
  ): L.Div =
    L.div(
      L.cls(Styles.header),
      showShortcutsButton(tooltip, modal),
      L.span(L.cls(Styles.name), planName),
      toHistoryButton(undoIcon, "Undo", "Ctrl+Z", undoController.undoLabel, tooltip)(
        undoController.undo()
      ),
      toHistoryButton(redoIcon, "Redo", "Ctrl+Shift+Z", undoController.redoLabel, tooltip)(
        undoController.redo()
      ),
      L.span(L.cls(Styles.separator)),
      toAddStepButton(focus, newStepDraft, tooltip),
      L.child <-- toDeleteStepButton(focus, stepDeleter, tooltip)
    )

  @js.native @JSImport("/styles/planning/plan/planHeader.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val header: String = js.native
    val iconButton: String = js.native
    val icon: String = js.native
    val name: String = js.native
    val separator: String = js.native
    val textButton: String = js.native
    val textButtonIcon: String = js.native
    val addStepButton: String = js.native
    val addStepLabel: String = js.native
    val deleteStepIcon: String = js.native
    val tooltip: String = js.native
    val deleteStepButton: String = js.native
    val disabledDeleteStepButtonExplainer: String = js.native
  }

  private def showShortcutsButton(tooltip: Tooltip, modal: Modal): L.Button = {
    val shortcutsModal = KeyboardShortcutsModal(modal)
    Button(_.handled --> (_ => shortcutsModal.open())).amend(
      L.cls(Styles.iconButton),
      FontAwesome.icon(FreeSolid.faKeyboard).amend(L.svg.cls(Styles.icon)),
      IconButtonModifiers(
        tooltipContents = "Show keyboard shortcuts",
        screenReaderDescription = "show keyboard shortcuts",
        tooltip,
        tooltipPlacement = Placement.bottom
      )
    )
  }

  // Font Awesome's undo arrows are filled curves, which look jagged at this size on screens
  // without high pixel density. These simpler stroked arrows stay smooth.
  private def undoIcon: L.SvgElement =
    historyIcon(arrowhead = "M5.5 2.5 2 6l3.5 3.5", shaft = "M2 6h8a4 4 0 0 1 0 8H6")

  private def redoIcon: L.SvgElement =
    historyIcon(arrowhead = "M10.5 2.5 14 6l-3.5 3.5", shaft = "M14 6H6a4 4 0 0 0 0 8h4")

  private def historyIcon(arrowhead: String, shaft: String): L.SvgElement =
    L.svg.svg(
      L.svg.viewBox("0 0 16 16"),
      L.svg.fill("none"),
      L.svg.stroke("currentColor"),
      L.svg.strokeWidth("2"),
      L.svg.strokeLineCap("round"),
      L.svg.strokeLineJoin("round"),
      L.svg.svgAttr("aria-hidden", StringAsIsCodec, namespace = None)("true"),
      L.svg.path(L.svg.d(arrowhead)),
      L.svg.path(L.svg.d(shaft))
    )

  /** @param label a description of the change the button would undo or redo, if there is one */
  private def toHistoryButton(
    icon: L.SvgElement,
    action: String,
    shortcut: String,
    label: Signal[Option[String]],
    tooltip: Tooltip
  )(run: => Unit): L.Button =
    Button(_.handled --> (_ => run)).amend(
      L.cls(Styles.iconButton),
      L.disabled <-- label.map(_.isEmpty),
      icon.amend(L.svg.cls(Styles.icon)),
      IconButtonModifiers.using(
        tooltipContents = label.map {
          case Some(change) => s"$action: $change ($shortcut)"
          case None => s"Nothing to ${action.toLowerCase}"
        },
        screenReaderDescription = Signal.fromValue(action.toLowerCase),
        tooltip,
        tooltipPlacement = Placement.bottom
      )
    )

  /** Shrinks to its icon when the plan is narrow */
  private def toAddStepButton(
    focus: Signal[Option[Step.ID]],
    newStepDraft: NewStepDraft,
    tooltip: Tooltip
  ): L.Button =
    Button(_.handledWith(_.sample(focus)) --> newStepDraft.open).amend(
      L.cls(Styles.addStepButton),
      L.aria.label("Add step"),
      FontAwesome.icon(FreeSolid.faPlus).amend(L.svg.cls(Styles.textButtonIcon)),
      L.span(L.cls(Styles.addStepLabel), "Add step"),
      tooltip.register(
        L.span(L.cls(Styles.tooltip), "Add step (N)"),
        FloatingConfig.basicTooltip(Placement.bottom)
      )
    )

  private def toDeleteStepButton(
    focus: Signal[Option[Step.ID]],
    stepDeleter: StepDeleter,
    tooltip: Tooltip
  ): Signal[L.Button] = {
    // Named in full, even when the plan is narrow, so it can't be mistaken for deleting the plan
    def description = List(
      FontAwesome.icon(FreeSolid.faTrashCan).amend(L.svg.cls(Styles.textButtonIcon, Styles.deleteStepIcon)),
      textToTextNode("Delete step")
    )

    focus.splitOption(
      project = (_, step) =>
        Button(_.handledWith(_.sample(step)) --> stepDeleter.delete).amend(
          L.cls(Styles.deleteStepButton),
          description
        ),
      ifEmpty =
        Button(_ --> Observer.empty).amend(
          L.cls(Styles.deleteStepButton),
          L.disabled(true),
          description,
          tooltip.register(
            L.span(
              L.cls(Styles.disabledDeleteStepButtonExplainer),
              "Focus a step to delete it"
            ),
            FloatingConfig.basicTooltip(Placement.bottom)
          )
        )
    )
  }
}
