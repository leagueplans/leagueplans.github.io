package com.leagueplans.ui.dom.plans.actions

import com.leagueplans.ui.dom.plans.PlanDialog.Styles
import com.leagueplans.ui.dom.plans.{PlanDialog, PlanManager, PlanName}
import com.leagueplans.uicommon.dom.{Button, Modal}
import com.leagueplans.uicommon.utils.laminar.EventProcessorOps.handled
import com.raquo.airstream.core.Observer
import com.raquo.airstream.state.{StrictSignal, Var}
import com.raquo.laminar.api.{L, eventPropToProcessor, textToTextNode}

/** Opens a dialog to delete the open plan, which only goes ahead once the plan's name has been
  * typed. Plans can't be recovered, and a double click shouldn't be enough to lose one.
  */
object DeleteButton {
  /** Says what deleting the plan means */
  def warning(name: String): String =
    s"“$name” will be deleted permanently. This can't be undone. Download it first if you want a backup."

  def apply(name: StrictSignal[String], manager: PlanManager, modal: Modal): L.Button =
    Button(_.handled --> Observer[Unit](_ => modal.show(toDialog(name.now(), manager, modal)))).amend(
      "Delete this plan…"
    )

  private def toDialog(name: String, manager: PlanManager, modal: Modal): L.FormElement = {
    val typed = Var("")
    val matches = typed.signal.map(PlanName.matches(_, name)).distinct

    PlanDialog("delete-plan-title", "Delete this plan?", modal)(
      PlanDialog.body(
        L.p(
          L.cls(Styles.error),
          warning(name)
        ),
        L.label(L.cls(Styles.label), L.forId("delete-plan-name"), "Type the plan's name to confirm"),
        L.input(
          L.cls(Styles.field),
          L.idAttr("delete-plan-name"),
          L.nameAttr("delete-plan-name"),
          L.`type`("text"),
          L.autoComplete("off"),
          L.placeholder(name),
          // The dialog focuses this as it opens, rather than its close button
          L.autoFocus(true),
          L.controlled(
            L.value <-- typed.signal,
            L.onInput.mapToValue --> typed.writer
          )
        )
      ),
      PlanDialog.footer(
        "The plan is deleted from this browser.",
        PlanDialog.cancelButton(modal),
        PlanDialog.submitButton("Delete plan", danger = true).amend(L.disabled <-- matches.map(!_))
      ),
      L.onSubmit.preventDefault --> Observer[Any] { _ =>
        if (PlanName.matches(typed.now(), name)) {
          modal.close()
          manager.deleteOpenPlan(name)
        }
      }
    )
  }
}
