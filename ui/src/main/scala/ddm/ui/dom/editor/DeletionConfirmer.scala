package ddm.ui.dom.editor

import com.raquo.airstream.core.{Observable, Observer}
import com.raquo.laminar.api.{L, textToTextNode}
import ddm.ui.dom.common.form.Form
import ddm.ui.dom.common.{CancelModalButton, FormOpener, Modal}

object DeletionConfirmer {
  def apply(
    modalController: Modal.Controller,
    deletionObserver: Observer[Unit]
  ): Observer[FormOpener.Command] =
    FormOpener(
      modalController,
      deletionObserver,
      () => createForm(modalController)
    )

  private def createForm(
    modalController: Modal.Controller
  ): (L.FormElement, Observable[Unit]) = {
    val (emptyForm, submitButton, formSubmissions) = Form()

    val form = emptyForm.amend(
      L.p(
        "Are you sure you want to delete this step?\n" +
          "This will also delete any nested steps."
      ),
      CancelModalButton(modalController).amend(L.onMountFocus),
      submitButton.amend(L.value("Confirm"))
    )

    (form, formSubmissions)
  }
}
