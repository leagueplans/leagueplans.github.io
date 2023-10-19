package ddm.ui.dom.editor

import com.raquo.airstream.core.{Observable, Observer, Sink}
import com.raquo.airstream.eventbus.WriteBus
import com.raquo.laminar.api.{L, textToTextNode}
import ddm.ui.dom.common.form.Form
import ddm.ui.dom.common.{CancelModalButton, FormOpener}

object DeletionConfirmer {
  def apply(
    modalBus: WriteBus[Option[L.Element]],
    deletionObserver: Sink[Unit]
  ): Observer[FormOpener.Command] =
    FormOpener(
      modalBus,
      deletionObserver,
      () => createForm(modalBus)
    )

  private def createForm(
    modalBus: WriteBus[Option[L.Element]]
  ): (L.FormElement, Observable[Unit]) = {
    val (emptyForm, submitButton, formSubmissions) = Form()

    val form = emptyForm.amend(
      L.p(
        "Are you sure you want to delete this step?\n" +
          "This will also delete any nested steps."
      ),
      CancelModalButton(modalBus).amend(L.onMountFocus),
      submitButton.amend(L.value("Confirm"))
    )

    (form, formSubmissions)
  }
}
