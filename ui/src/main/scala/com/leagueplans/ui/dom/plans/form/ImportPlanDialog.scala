package com.leagueplans.ui.dom.plans.form

import com.leagueplans.ui.dom.plans.PlanDialog.Styles
import com.leagueplans.ui.dom.plans.form.PlanFileImport.Selection
import com.leagueplans.ui.dom.plans.{PlanDialog, PlanManager}
import com.leagueplans.ui.storage.model.PlanExport
import com.leagueplans.uicommon.dom.{Button, Modal}
import com.leagueplans.uicommon.utils.laminar.EventProcessorOps.handled
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, enrichSource, eventPropToProcessor, textToTextNode}
import org.scalajs.dom.File

/** Asks for a plan file, and saves its plan as it was made: the name and settings come from the
  * file. The plan manager opens it.
  */
object ImportPlanDialog {
  def open(manager: PlanManager, modal: Modal): Unit =
    modal.show(apply(manager, modal))

  private def apply(manager: PlanManager, modal: Modal): L.FormElement = {
    val file = Var(Option.empty[File])
    val isDragging = Var(false)
    val isImporting = Var(false)
    val failure = Var(Option.empty[String])
    val requests = EventBus[PlanExport]()

    val input = PlanFileImport.input(file)
    val choose = Observer[Unit](_ => input.ref.click())

    val selection = PlanFileImport.read(file.signal)

    val importable = selection.map {
      case Selection.Readable(_, plan) => Some(plan)
      case _ => None
    }

    PlanDialog("import-plan-title", "Import a plan file", modal)(
      PlanDialog.body(
        input,
        L.child <-- selection.map {
          case Selection.Nothing => toDropTarget(isDragging.signal, choose)
          case Selection.Reading(file) => toFileCard(file, "Reading…", choose)
          case Selection.Readable(file, _) => toFileCard(file, "Ready to import.", choose)
          case Selection.Unreadable(file, _) => toFileCard(file, "Can't be read.", choose)
        },
        L.child.maybe <-- selection.map {
          case Selection.Unreadable(_, reason) => Some(toError(reason))
          case _ => None
        },
        L.child.maybe <-- failure.signal.map(_.map(toError)),
        L.span(
          L.cls(Styles.hint),
          "You can change the name and mode of the plan in Settings."
        )
      ),
      PlanDialog.footer(
        "Nothing is uploaded: the file is saved to your browser.",
        PlanDialog.cancelButton(modal),
        PlanDialog.submitButton("Import plan").amend(
          L.disabled <-- importable.combineWith(isImporting.signal).map((plan, importing) =>
            importing || plan.isEmpty
          )
        )
      ),
      // A file dropped anywhere in the dialog counts
      PlanFileImport.dropTarget(file, isDragging, failure),
      L.onSubmit.preventDefault.compose(_.sample(importable).collectSome) --> Observer[PlanExport] { plan =>
        if (!isImporting.now()) {
          isImporting.set(true)
          failure.set(None)
          requests.emit(plan)
        }
      },
      requests.events.flatMapSwitch(PlanFileImport.save(manager, _)) --> Observer[Either[String, Unit]] {
        case Right(()) =>
          modal.close()

        case Left(reason) =>
          isImporting.set(false)
          failure.set(Some(reason))
      }
    )
  }

  private def toDropTarget(isDragging: Signal[Boolean], choose: Observer[Unit]): L.Div =
    L.div(
      L.cls(Styles.drop),
      L.cls(Styles.dragging) <-- isDragging,
      L.div(L.cls(Styles.dropTitle), "Drop a plan file here"),
      L.div(L.cls(Styles.hint), ".plan or .plan.gz, from Download in Settings, or from a friend"),
      Button(_.handled --> choose).amend(L.cls(Styles.button), "Choose file…")
    )

  private def toFileCard(file: File, status: String, choose: Observer[Unit]): L.Div =
    L.div(
      L.cls(Styles.file),
      L.span(L.cls(Styles.fileName), file.name),
      L.span(L.cls(Styles.hint), status),
      Button(_.handled --> choose).amend(L.cls(Styles.button), "Change")
    )

  private def toError(reason: String): L.HtmlElement =
    L.p(L.cls(Styles.error), L.role("alert"), reason)
}
