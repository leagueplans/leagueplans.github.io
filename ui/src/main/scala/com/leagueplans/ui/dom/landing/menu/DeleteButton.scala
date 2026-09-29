package com.leagueplans.ui.dom.landing.menu

import com.leagueplans.ui.dom.planning.plan.CollapsedSteps
import com.leagueplans.ui.storage.client.StorageClient
import com.leagueplans.ui.storage.model.PlanID
import com.leagueplans.uicommon.dom.*
import com.leagueplans.uicommon.facades.floatingui.Placement
import com.leagueplans.uicommon.facades.fontawesome.freesolid.FreeSolid
import com.leagueplans.uicommon.utils.airstream.PromiseLikeOps.onComplete
import com.leagueplans.uicommon.utils.laminar.EventProcessorOps.handled
import com.leagueplans.uicommon.utils.laminar.FontAwesome
import com.raquo.airstream.core.Observer
import com.raquo.laminar.api.{L, textToTextNode}

import scala.concurrent.duration.DurationInt

object DeleteButton {
  def apply(
    id: PlanID,
    name: String,
    storage: StorageClient,
    tooltip: Tooltip,
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
        tooltipContents = "Delete",
        screenReaderDescription = "delete",
        tooltip,
        tooltipPlacement = Placement.left
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
      onSuccess = _ => CollapsedSteps.forget(id)
    )
}
