package com.leagueplans.ui.dom.landing.menu

import com.leagueplans.ui.dom.common.*
import com.leagueplans.ui.facades.fontawesome.freesolid.FreeSolid
import com.leagueplans.ui.storage.client.StorageClient
import com.leagueplans.ui.storage.model.PlanID
import com.leagueplans.ui.utils.airstream.PromiseLikeOps.onComplete
import com.leagueplans.ui.utils.laminar.FontAwesome
import com.leagueplans.ui.utils.laminar.EventProcessorOps.handled
import com.raquo.airstream.core.Observer
import com.raquo.laminar.api.{L, textToTextNode}

import scala.concurrent.duration.DurationInt

object DeleteButton {
  def apply(
    id: PlanID,
    name: String,
    storage: StorageClient,
    toastPublisher: ToastHub.Publisher,
    modal: Modal
  ): L.Button = {
    val confirmer = DeletionConfirmer(
      s"\"$name\" will be permanently deleted. This cannot be undone.",
      "Delete plan",
      modal,
      Observer(_ => triggerDelete(id, storage, toastPublisher))
    )

    Button(_.handled --> confirmer).amend(
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
