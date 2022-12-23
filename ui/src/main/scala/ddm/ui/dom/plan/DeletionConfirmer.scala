package ddm.ui.dom.plan

import com.raquo.airstream.core.{Observable, Observer, Sink}
import com.raquo.airstream.eventbus.WriteBus
import com.raquo.laminar.api.{L, eventPropToProcessor, textToNode}
import ddm.ui.dom.common.FormOpener
import ddm.ui.dom.common.form.Form
import ddm.ui.utils.laminar.LaminarOps.RichL
import org.scalajs.dom.MouseEvent

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
          "This will also delete any nested steps as well."
      ),
      cancelButton(modalBus),
      submitButton.amend(L.value("Confirm"))
    )

    (form, formSubmissions)
  }

  private def cancelButton(modalBus: WriteBus[Option[L.Element]]): L.Button =
    L.button(
      L.`type`("button"),
      "Cancel",
      L.onMountFocus,
      L.ifUnhandled(L.onClick) --> modalBus.contramap[MouseEvent] { event =>
        event.preventDefault()
        None
      }
    )
}
