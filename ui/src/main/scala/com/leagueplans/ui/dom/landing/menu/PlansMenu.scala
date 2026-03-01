package com.leagueplans.ui.dom.landing.menu

import com.leagueplans.ui.dom.common.{Modal, ToastHub, Tooltip}
import com.leagueplans.ui.model.plan.Plan
import com.leagueplans.ui.storage.client.{PlanSubscription, StorageClient}
import com.leagueplans.ui.storage.model.{PlanID, PlanMetadata, SchemaVersion}
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, StringSeqValueMapper}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html.OList

import scala.scalajs.js
import scala.scalajs.js.Date
import scala.scalajs.js.annotation.JSImport

object PlansMenu {
  def apply(
    storage: StorageClient,
    selectionObserver: Observer[(Plan, PlanSubscription)],
    tooltip: Tooltip,
    toastPublisher: ToastHub.Publisher,
    modal: Modal
  ): ReactiveHtmlElement[OList] =
    L.ol(
      L.cls(Styles.list),
      L.children <-- toEntries(storage, selectionObserver, tooltip, toastPublisher, modal)
    )

  @js.native @JSImport("/styles/landing/menu/plansMenu.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val list: String = js.native
    val row: String = js.native
    val descriptor: String = js.native
    val button: String = js.native
    val downloadButton: String = js.native
    val deleteButton: String = js.native
    val loadButton: String = js.native
    val updateButton: String = js.native
  }

  private def toEntries(
    storage: StorageClient,
    selectionObserver: Observer[(Plan, PlanSubscription)],
    tooltip: Tooltip,
    toastPublisher: ToastHub.Publisher,
    modal: Modal
  ): Signal[List[L.LI]] =
    storage
      .plansSignal
      .map(sort)
      .split((id, _) => id) { case (_, (id, metadata), _) =>
        toEntry(id, metadata, storage, selectionObserver, tooltip, toastPublisher, modal)
      }

  private given Ordering[Date] =
    Ordering.by[Date, Double](_.getTime()).reverse

  private def sort(plans: Map[PlanID, PlanMetadata]): List[(PlanID, PlanMetadata)] =
    plans.toList.sortBy((_, metadata) => (metadata.timestamp, metadata.name))

  private def toEntry(
    id: PlanID,
    metadata: PlanMetadata,
    storage: StorageClient,
    selectionObserver: Observer[(Plan, PlanSubscription)],
    tooltip: Tooltip,
    toastPublisher: ToastHub.Publisher,
    modal: Modal
  ): L.LI =
    L.li(
      L.cls(Styles.row),
      DeleteButton(id, metadata.name, storage, tooltip, toastPublisher, modal).amend(
        L.cls(Styles.button, Styles.deleteButton)
      ),
      PlanDescriptor(metadata).amend(L.cls(Styles.descriptor)),
      DownloadButton(id, metadata.name, storage, tooltip, toastPublisher).amend(
        L.cls(Styles.button, Styles.downloadButton)
      ),
      if (metadata.schemaVersion == SchemaVersion.values.last)
        LoadButton(id, storage, selectionObserver, toastPublisher).amend(
          L.cls(Styles.button, Styles.loadButton)
        )
      else
        UpdateButton(id, storage, toastPublisher).amend(
          L.cls(Styles.button, Styles.updateButton)
        )
    )
}
