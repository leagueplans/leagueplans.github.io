package com.leagueplans.ui.dom.planning.editor.repetitions

import com.leagueplans.ui.dom.common.form.{Form, NumberInput}
import com.leagueplans.ui.dom.common.{CancelModalButton, FormOpener, Modal}
import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.ui.utils.laminar.LaminarOps.selectOnFocus
import com.raquo.airstream.core.{EventStream, Observer}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, StringSeqValueMapper, enrichSource, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object EditRepetitionsForm {
  def apply(forester: Forester[Step.ID, Step], modal: Modal): EditRepetitionsForm = {
    val (form, submitButton, formSubmissions) = Form()

    val repetitionsVar = Var(1)
    val showAncestorNote = Var(false)

    val (repetitionsInput, repetitionsLabel) = NumberInput("step-repetitions-input", repetitionsVar)
    repetitionsLabel.amend(L.cls(Styles.label), "Repetitions")
    repetitionsInput.amend(
      L.cls(Styles.input),
      L.required(true),
      L.minAttr("1"),
      L.inContext(node =>
        repetitionsVar --> {
          case n if n > 10000 =>
            node.ref.setCustomValidity("High repetition counts will cause poor UI performance (max 10,000)")
          case _ =>
            node.ref.setCustomValidity("")
        }
      ),
      L.stepAttr("1"),
      L.selectOnFocus
    )

    form.amend(
      L.cls(Styles.form, Modal.Styles.form),
      L.p(L.cls(Styles.title, Modal.Styles.title), "How many times should this step repeat?"),
      L.sectionTag(L.cls(Styles.inputs), repetitionsLabel, repetitionsInput),
      L.p(
        L.cls(Styles.note),
        "A step which repeats runs in a loop with its substeps. ",
      ),
      L.p(
        L.cls(Styles.note),
        "Large repetition counts can slow down the UI. If you notice poor performance," +
          " consider collapsing a repeating step into a single step that runs once. For" +
          " example, instead of 1000 repetitions of a step granting 5 xp, use a single" +
          " step granting 5,000 xp."
      ),
      L.div(
        L.cls(Styles.buttons),
        CancelModalButton(modal).amend(L.cls(Styles.cancel, Modal.Styles.confirmationButton)),
        submitButton.amend(
          L.cls(Styles.confirm, Modal.Styles.confirmationButton),
          L.value("Set repetitions")
        )
      )
    )

    val submissions = formSubmissions.sample(repetitionsVar.signal)

    new EditRepetitionsForm(forester, modal, form, submissions, repetitionsVar.writer, showAncestorNote.writer)
  }

  @js.native @JSImport("/styles/planning/editor/repetitions/editRepetitionsForm.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val form: String = js.native
    val title: String = js.native
    val inputs: String = js.native
    val label: String = js.native
    val input: String = js.native
    val note: String = js.native
    val buttons: String = js.native
    val cancel: String = js.native
    val confirm: String = js.native
  }
}

final class EditRepetitionsForm private(
  forester: Forester[Step.ID, Step],
  modal: Modal,
  form: L.FormElement,
  submissions: EventStream[Int],
  repetitions: Observer[Int],
  showAncestorNote: Observer[Boolean]
) {
  private var stepID = Option.empty[Step.ID]
  private val opener = FormOpener(
    modal,
    form,
    submissions,
    count => stepID.foreach(
      forester.update(_, _.deepCopy(repetitions = count))
    )
  )

  def open(step: Step): Unit = {
    stepID = Some(step.id)
    repetitions.onNext(step.repetitions)
    showAncestorNote.onNext {
      val forest = forester.signal.now()
      forest.ancestors(step.id).flatMap(forest.get).exists(_.repetitions > 1)
    }
    opener.open()
  }
}