package ddm.ui.dom.plans

import cats.data.NonEmptyList
import com.raquo.airstream.core.{EventStream, Observer, Signal}
import com.raquo.laminar.api.{L, enrichSource, textToTextNode}
import ddm.ui.dom.common.{InfoIcon, Tooltip}
import ddm.ui.dom.common.form.{Form, JsonFileInput, Select, TextInput}
import ddm.ui.model.common.forest.{Forest, Tree}
import ddm.ui.model.plan.{SavedState, Step}
import ddm.ui.model.player.mode.Mode

import java.util.UUID
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object NewPlanForm {
  def apply(existingPlansSignal: Signal[Set[String]]): (L.FormElement, EventStream[SavedState.Named]) = {
    val (emptyForm, submitButton, formSubmissions) = Form()
    val (nameInput, nameLabel, nameSignal) = toNameInput(existingPlansSignal)
    val (modeSelect, modeLabel, modeSignal) = createModeSelect()
    val (importInput, importLabel, importSignal) = createImportInput()

    val form = emptyForm.amend(
      L.cls(Styles.form),
      nameLabel.amend(L.cls(Styles.label), "Name:"),
      nameInput.amend(L.cls(Styles.input)),
      modeLabel.amend(L.cls(Styles.label), "Game mode:"),
      modeSelect.amend(L.cls(Styles.input)),
      L.div(
        InfoIcon().amend(L.svg.cls(Styles.infoIcon)),
        importLabel.amend(L.cls(Styles.label), "Initial data:"),
        Tooltip(L.p(
          L.cls(Styles.tooltip),
          "If you have a save file for an existing plan, then you can import it here. Otherwise, you can ignore this."
        ))
      ),
      importInput.amend(L.cls(Styles.input)),
      submitButton.amend(L.cls(Styles.submit), L.value("Create plan"))
    )
    val submissions = planSubmissions(formSubmissions, nameSignal, modeSignal, importSignal)

    (form, submissions)
  }

  @js.native @JSImport("/styles/plans/newPlanForm.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val form: String = js.native
    val label: String = js.native
    val input: String = js.native
    val submit: String = js.native

    val infoIcon: String = js.native
    val tooltip: String = js.native
  }

  private def toNameInput(existingPlansSignal: Signal[Set[String]]): (L.Input, L.Label, Signal[String]) = {
    val (baseInput, nameLabel, nameSignal) = TextInput(
      TextInput.Type.Text,
      id = "new-plan-name-entry",
      initial = ""
    )

    val nameInput = baseInput.amend(
      L.required(true),
      L.inContext(node =>
        Signal.combine(nameSignal, existingPlansSignal) --> Observer[(String, Set[String])]((input, existing) =>
          if (existing.contains(input))
            node.ref.setCustomValidity("Plan names must be unique")
          else
            node.ref.setCustomValidity("")
        )
      )
    )

    (nameInput, nameLabel, nameSignal)
  }

  private def createModeSelect(): (L.Select, L.Label, Signal[Mode]) =
    Select(
      id = "new-plan-mode-select",
      NonEmptyList.fromListUnsafe(Mode.all).map(mode =>
        Select.Opt(mode, mode.name)
      )
    )

  private def createImportInput(): (L.Input, L.Label, Signal[Option[SavedState]]) =
    JsonFileInput[SavedState](id = "import-existing-plan-input")

  private def planSubmissions(
    formSubmissions: EventStream[Unit],
    nameSignal: Signal[String],
    modeSignal: Signal[Mode],
    importSignal: Signal[Option[SavedState]]
  ): EventStream[SavedState.Named] =
    formSubmissions
      .sample(Signal.combine(nameSignal, modeSignal, importSignal))
      .map {
        case (name, mode, None) =>
          val steps = Forest.fromTrees[UUID, Step](
            List(Tree(Step(name), children = List.empty)),
            _.id
          )
          SavedState.Named(name, SavedState(mode, steps))

        case (name, mode, Some(state)) =>
          SavedState.Named(name, state.copy(mode = mode))
      }
}
