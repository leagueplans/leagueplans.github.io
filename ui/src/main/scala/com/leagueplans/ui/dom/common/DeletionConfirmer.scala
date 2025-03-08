package com.leagueplans.ui.dom.common

import com.leagueplans.ui.dom.common.form.Form
import com.raquo.airstream.core.{Observable, Observer}
import com.raquo.laminar.api.{L, StringSeqValueMapper, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object DeletionConfirmer {
  def apply(
    warningText: String,
    deleteButtonText: String,
    modal: Modal,
    deletionObserver: Observer[Unit]
  ): FormOpener =
    FormOpener(
      modal,
      createForm(warningText, deleteButtonText, modal),
      deletionObserver
    )

  @js.native @JSImport("/styles/common/deletionConfirmer.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val form: String = js.native
    val header: String = js.native
    val description: String = js.native
    val buttons: String = js.native
    val button: String = js.native
    val cancel: String = js.native
    val confirm: String = js.native
  }

  private def createForm(
    warningText: String,
    deleteButtonText: String,
    modal: Modal
  ): (L.FormElement, Observable[Unit]) = {
    val (emptyForm, submitButton, formSubmissions) = Form()

    val form = emptyForm.amend(
      L.cls(Styles.form),
      L.div(L.cls(Styles.header), "Please confirm deletion"),
      L.p(
        L.cls(Styles.description),
        warningText
      ),
      L.div(
        L.cls(Styles.buttons),
        CancelModalButton(modal).amend(
          L.cls(Styles.cancel, Styles.button),
          L.onMountFocus
        ),
        submitButton.amend(
          L.cls(Styles.confirm, Styles.button),
          L.value(deleteButtonText)
        )
      )
    )

    (form, formSubmissions)
  }
}
