package ddm.ui.dom.plans

import cats.data.NonEmptyList
import com.raquo.airstream.core.{EventStream, Observer, Signal}
import com.raquo.laminar.api.{L, enrichSource, textToTextNode}
import ddm.ui.dom.common.form.{Form, Select, TextInput}
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

    val form = emptyForm.amend(
      L.cls(Styles.form),
      nameLabel.amend(L.cls(Styles.label), "Name:"),
      nameInput.amend(L.cls(Styles.input)),
      modeLabel.amend(L.cls(Styles.label), "Game mode:"),
      modeSelect.amend(L.cls(Styles.input)),
      submitButton.amend(L.cls(Styles.submit), L.value("Create plan"))
    )
    val submissions = planSubmissions(formSubmissions, nameSignal, modeSignal)

    (form, submissions)
  }

  @js.native @JSImport("/styles/plans/newPlanForm.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val form: String = js.native
    val label: String = js.native
    val input: String = js.native
    val submit: String = js.native
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
        Signal.combine(nameSignal, existingPlansSignal) --> Observer[(String, Set[String])] { case (input, existing) =>
          if (existing.contains(input))
            node.ref.setCustomValidity("Plan names must be unique")
          else
            node.ref.setCustomValidity("")
        }
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

  private def planSubmissions(
    formSubmissions: EventStream[Unit],
    nameSignal: Signal[String],
    modeSignal: Signal[Mode]
  ): EventStream[SavedState.Named] =
    formSubmissions
      .sample(Signal.combine(nameSignal, modeSignal))
      .map { case (name, mode) =>
        val steps = Forest.fromTrees[UUID, Step](
          List(Tree(Step(name), children = List.empty)),
          _.id
        )
        SavedState.Named(name, SavedState(mode, steps))
      }
}
