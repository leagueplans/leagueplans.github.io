package com.leagueplans.ui.dom.planning.editor.time

import com.leagueplans.ui.dom.common.form.{Form, NumberInput, Select}
import com.leagueplans.ui.dom.common.{CancelModalButton, FormOpener, Modal}
import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.model.plan.{Duration, Step}
import com.leagueplans.ui.utils.laminar.LaminarOps.selectOnFocus
import com.raquo.airstream.core.{EventStream, Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, StringSeqValueMapper, enrichSource, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object EditDurationForm {
  def apply(forester: Forester[Step.ID, Step], modal: Modal): EditDurationForm = {
    val (form, submitButton, formSubmissions) = Form()

    val lengthVar = Var(0)
    val unitVar = Var(Duration.Unit.Seconds)
    val showSubstepWarning = Var(false)

    val (lengthInput, lengthLabel) = NumberInput("step-duration-length-input", lengthVar)
    lengthLabel.amend(L.cls(Styles.lengthLabel), "Expected length")
    lengthInput.amend(
      L.cls(Styles.lengthInput),
      L.required(true),
      L.minAttr("0"),
      L.inContext(node =>
        Signal.combine(lengthVar, unitVar) --> { (length, unit) =>
          val max = maxLength(unit)
          val message =
            if (length > max)
              s"Buddy, you're getting nerd-logged. Break this up with a \"log back" +
                s" in\" step (max $max ${unit.toString.toLowerCase})"
            else
              ""

          node.ref.setCustomValidity(message)
        }
      ),
      L.stepAttr("1"),
      L.selectOnFocus
    )

    val (unitSelect, unitLabel) =
      Select(
        "step-duration-unit-input",
        List(
          Select.Opt(Duration.Unit.Seconds, "seconds"),
          Select.Opt(Duration.Unit.Ticks, "ticks")
        ),
        unitVar
      )
    unitLabel.amend(L.cls(Styles.unitLabel), "Seconds or ticks?")
    unitSelect.amend(L.cls(Styles.unitInput))

    form.amend(
      L.cls(Styles.form, Modal.Styles.form),
      L.p(L.cls(Styles.title, Modal.Styles.title), "How long do you expect this step to take?"),
      L.sectionTag(
        L.cls(Styles.inputs),
        lengthLabel,
        lengthInput,
        unitLabel,
        unitSelect
      ),
      L.p(
        L.cls(Styles.note),
        "This should be the duration of this step alone, ignoring substeps or repetitions."
      ),
      L.div(
        L.cls(Styles.buttons),
        CancelModalButton(modal).amend(L.cls(Styles.cancel, Modal.Styles.confirmationButton)),
        submitButton.amend(
          L.cls(Styles.confirm, Modal.Styles.confirmationButton),
          L.value("Set duration")
        )
      )
    )

    val submissions = formSubmissions
      .sample(Signal.combine(lengthVar.signal, unitVar.signal))
      .map { case (len, unit) => Duration(len, unit) }

    new EditDurationForm(
      forester,
      modal,
      form,
      submissions,
      lengthVar.writer,
      unitVar.writer,
      showSubstepWarning.writer
    )
  }

  private def maxLength(unit: Duration.Unit): Int =
    unit match {
      case Duration.Unit.Ticks => 36000
      case Duration.Unit.Seconds => 21600
    }

  @js.native @JSImport("/styles/planning/editor/time/editDurationForm.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val form: String = js.native
    val title: String = js.native
    val inputs: String = js.native
    val lengthLabel: String = js.native
    val lengthInput: String = js.native
    val unitLabel: String = js.native
    val unitInput: String = js.native
    val note: String = js.native
    val buttons: String = js.native
    val cancel: String = js.native
    val confirm: String = js.native
  }
}

final class EditDurationForm private(
  forester: Forester[Step.ID, Step],
  modal: Modal,
  form: L.FormElement,
  submissions: EventStream[Duration],
  length: Observer[Int],
  timeUnit: Observer[Duration.Unit],
  showSubstepWarning: Observer[Boolean]
) {
  private var stepID = Option.empty[Step.ID]
  private val opener = FormOpener(
    modal,
    form,
    submissions,
    duration => stepID.foreach(
      forester.update(_, _.deepCopy(duration = duration))
    )
  )

  def open(step: Step): Unit = {
    stepID = Some(step.id)
    if (step.duration.length != 0) {
      length.onNext(step.duration.length)
      timeUnit.onNext(step.duration.unit)
    }
    showSubstepWarning.onNext(
      forester.signal.now().children(step.id).nonEmpty
    )
    opener.open()
  }
}
