package com.leagueplans.ui.dom.plans.form

import com.leagueplans.ui.dom.plans.PlanDialog.Styles
import com.leagueplans.ui.dom.plans.{ModeSelect, PlanDialog, PlanManager, PlanName}
import com.leagueplans.ui.model.player.mode.{MainGame, Mode}
import com.leagueplans.ui.storage.model.errors.FileSystemError
import com.leagueplans.ui.storage.model.PlanID
import com.leagueplans.uicommon.dom.Modal
import com.raquo.airstream.core.Observer
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, enrichSource, eventPropToProcessor, textToTextNode}

/** Asks for a name and a game mode, and makes an empty plan with them. The plan manager opens it. */
object NewPlanDialog {
  def open(manager: PlanManager, modal: Modal): Unit =
    modal.show(apply(manager, modal))

  private def apply(manager: PlanManager, modal: Modal): L.FormElement = {
    val name = Var("")
    val mode = Var[Mode](MainGame)
    val isCreating = Var(false)
    val failure = Var(Option.empty[String])
    val requests = EventBus[(String, Mode)]()

    val (modeSelect, modeLabel) = ModeSelect("new-plan-mode", mode)

    PlanDialog("new-plan-title", "New plan", modal)(
      PlanDialog.body(
        L.label(L.cls(Styles.label), L.forId("new-plan-name"), "Name"),
        L.input(
          L.cls(Styles.field),
          L.idAttr("new-plan-name"),
          L.nameAttr("new-plan-name"),
          L.`type`("text"),
          L.autoComplete("off"),
          L.required(true),
          // The dialog focuses this as it opens, rather than its close button
          L.autoFocus(true),
          L.controlled(
            L.value <-- name.signal,
            L.onInput.mapToValue --> name.writer
          )
        ),
        modeLabel.amend(L.cls(Styles.label), "Game mode"),
        modeSelect.amend(L.cls(Styles.field)),
        L.span(L.cls(Styles.hint), "You can rename the plan or change its mode later, in Settings."),
        L.child.maybe <-- failure.signal.map(_.map(message =>
          L.p(L.cls(Styles.error), L.role("alert"), message)
        ))
      ),
      PlanDialog.footer(
        "Saved in this browser only.",
        PlanDialog.cancelButton(modal),
        PlanDialog.submitButton("Create plan").amend(
          L.disabled <-- name.signal.combineWith(isCreating.signal).map((name, creating) =>
            creating || PlanName.clean(name).isEmpty
          )
        )
      ),
      L.onSubmit.preventDefault --> Observer[Any] { _ =>
        PlanName.clean(name.now()).filterNot(_ => isCreating.now()).foreach { cleaned =>
          isCreating.set(true)
          failure.set(None)
          requests.emit((cleaned, mode.now()))
        }
      },
      requests.events.flatMapSwitch((name, mode) =>
        NewPlan.create(manager, name, mode)
      ) --> Observer[Either[FileSystemError, PlanID]] {
        case Right(_) =>
          modal.close()

        case Left(error) =>
          isCreating.set(false)
          failure.set(Some(s"Couldn't create the plan: ${error.message}"))
      }
    )
  }
}
