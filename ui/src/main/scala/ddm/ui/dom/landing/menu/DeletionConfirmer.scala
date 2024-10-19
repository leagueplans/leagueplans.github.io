package ddm.ui.dom.landing.menu

import com.raquo.airstream.core.{Observable, Observer}
import com.raquo.laminar.api.{L, StringSeqValueMapper, textToTextNode}
import ddm.ui.dom.common.form.Form
import ddm.ui.dom.common.{CancelModalButton, FormOpener, Modal}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object DeletionConfirmer {
  def apply(
    name: String,
    modalController: Modal.Controller,
    deletionObserver: Observer[Unit]
  ): Observer[FormOpener.Command] =
    FormOpener(
      modalController,
      deletionObserver,
      () => createForm(name, modalController)
    )

  @js.native @JSImport("/styles/landing/menu/deletionConfirmer.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val header: String = js.native
    val description: String = js.native
    val buttons: String = js.native
    val button: String = js.native
    val cancel: String = js.native
    val confirm: String = js.native
  }

  private def createForm(
    name: String,
    modalController: Modal.Controller
  ): (L.FormElement, Observable[Unit]) = {
    val (emptyForm, submitButton, formSubmissions) = Form()

    val form = emptyForm.amend(
      L.div(L.cls(Styles.header), "Please confirm deletion"),
      L.p(
        L.cls(Styles.description),
        s"\"$name\" will be permanently deleted. This cannot be undone."
      ),
      L.div(
        L.cls(Styles.buttons),
        CancelModalButton(modalController).amend(
          L.cls(Styles.cancel, Styles.button),
          L.onMountFocus
        ),
        submitButton.amend(
          L.cls(Styles.confirm, Styles.button),
          L.value("Delete plan")
        )
      )
    )

    (form, formSubmissions)
  }
}
