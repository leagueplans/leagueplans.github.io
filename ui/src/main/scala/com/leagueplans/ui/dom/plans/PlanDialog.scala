package com.leagueplans.ui.dom.plans

import com.leagueplans.uicommon.dom.{Button, Modal}
import com.leagueplans.uicommon.utils.laminar.EventProcessorOps.handledAs
import com.raquo.laminar.api.{L, seqToModifier, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** The frame shared by the dialogs for making, importing and deleting plans, in the style of the
  * keyboard shortcuts' dialog
  */
object PlanDialog {
  @js.native @JSImport("/styles/plans/planDialog.module.css", JSImport.Default)
  object Styles extends js.Object {
    val dialog: String = js.native
    val titleBar: String = js.native
    val title: String = js.native
    val close: String = js.native

    val body: String = js.native
    val label: String = js.native
    val field: String = js.native
    val hint: String = js.native
    val note: String = js.native
    val error: String = js.native

    val drop: String = js.native
    val dragging: String = js.native
    val dropTitle: String = js.native
    val file: String = js.native
    val fileName: String = js.native

    val footer: String = js.native
    val notice: String = js.native
    val button: String = js.native
    val primary: String = js.native
    val danger: String = js.native
  }

  /** A form, so that Enter submits it. The caller listens for the submissions. */
  def apply(titleID: String, title: String, modal: Modal)(content: L.Modifier[L.FormElement]*): L.FormElement =
    L.form(
      L.cls(Styles.dialog),
      L.aria.labelledBy(titleID),
      L.div(
        L.cls(Styles.titleBar),
        L.h2(L.cls(Styles.title), L.idAttr(titleID), title),
        Button(_.handledAs(()) --> (_ => modal.close())).amend(
          L.cls(Styles.close),
          L.aria.label("Close"),
          closeIcon()
        )
      ),
      content
    )

  def body(content: L.Modifier[L.Div]*): L.Div =
    L.div(L.cls(Styles.body), content)

  /** @param notice a line on the left, about what the dialog does with what's entered */
  def footer(notice: String, buttons: L.Modifier[L.Div]*): L.Div =
    L.div(L.cls(Styles.footer), L.span(L.cls(Styles.notice), notice), buttons)

  def cancelButton(modal: Modal): L.Button =
    Button(_.handledAs(()) --> (_ => modal.close())).amend(L.cls(Styles.button), "Cancel")

  /** The button that submits the dialog's form */
  def submitButton(label: String, danger: Boolean = false): L.Button =
    Button.submit().amend(
      L.cls(Styles.button),
      L.cls(if (danger) Styles.danger else Styles.primary),
      label
    )

  private def closeIcon(): L.SvgElement =
    L.svg.svg(
      L.svg.width("14"),
      L.svg.height("14"),
      L.svg.viewBox("0 0 16 16"),
      L.svg.fill("none"),
      L.svg.stroke("currentColor"),
      L.svg.strokeWidth("2"),
      L.svg.strokeLineCap("round"),
      L.svg.path(L.svg.d("M3 3l10 10M13 3L3 13"))
    )
}
