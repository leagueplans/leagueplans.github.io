package ddm.ui.dom.landing.menu

import com.raquo.airstream.core.Observer
import com.raquo.laminar.api.{L, textToTextNode}
import ddm.ui.dom.common.{Button, DeletionConfirmer, IconButtonModifiers, Modal, ToastHub}
import ddm.ui.facades.fontawesome.freesolid.FreeSolid
import ddm.ui.storage.client.StorageClient
import ddm.ui.storage.model.PlanID
import ddm.ui.utils.airstream.PromiseLikeOps.onComplete
import ddm.ui.utils.laminar.FontAwesome
import ddm.ui.utils.laminar.LaminarOps.handled

import scala.concurrent.duration.DurationInt

object DeleteButton {
  def apply(
    id: PlanID,
    name: String,
    storage: StorageClient,
    toastPublisher: ToastHub.Publisher,
    modalController: Modal.Controller
  ): L.Button = {
    val confirmer = DeletionConfirmer(
      s"\"$name\" will be permanently deleted. This cannot be undone.",
      "Delete plan",
      modalController,
      Observer(_ => triggerDelete(id, storage, toastPublisher))
    )

    Button(confirmer)(_.handled).amend(
      FontAwesome.icon(FreeSolid.faXmark),
      IconButtonModifiers(
        tooltip = "Delete",
        screenReaderDescription = "delete"
      )
    )
  }

  private def triggerDelete(
    id: PlanID,
    storage: StorageClient,
    toastPublisher: ToastHub.Publisher
  ): Unit =
    storage.delete(id).onComplete(
      error => toastPublisher.publish(
        ToastHub.Type.Warning,
        15.seconds,
        s"Failed to delete plan. Cause: [${error.message}]"
      ),
      onSuccess = _ => ()
    )
}
