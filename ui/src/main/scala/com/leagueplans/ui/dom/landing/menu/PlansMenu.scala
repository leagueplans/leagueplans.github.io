package com.leagueplans.ui.dom.landing.menu

import com.leagueplans.ui.dom.common.{Modal, ToastHub}
import com.leagueplans.ui.model.plan.Plan
import com.leagueplans.ui.storage.client.{PlanSubscription, StorageClient}
import com.leagueplans.ui.storage.model.{PlanID, PlanMetadata}
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, StringSeqValueMapper}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.HTMLOListElement

import scala.scalajs.js
import scala.scalajs.js.Date
import scala.scalajs.js.annotation.JSImport

object PlansMenu {
  def apply(
    storage: StorageClient,
    selectionObserver: Observer[(Plan, PlanSubscription)],
    toastPublisher: ToastHub.Publisher,
    modalController: Modal.Controller
  ): ReactiveHtmlElement[HTMLOListElement] =
    L.ol(
      L.cls(Styles.list),
      L.children <-- toEntries(storage, selectionObserver, toastPublisher, modalController)
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
  }

  private def toEntries(
    storage: StorageClient,
    selectionObserver: Observer[(Plan, PlanSubscription)],
    toastPublisher: ToastHub.Publisher,
    modalController: Modal.Controller
  ): Signal[List[L.LI]] =
    storage
      .plansSignal
      .map(sort)
      .split((id, _) => id) { case (_, (id, metadata), _) =>
        toEntry(id, metadata, storage, selectionObserver, toastPublisher, modalController)
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
    toastPublisher: ToastHub.Publisher,
    modalController: Modal.Controller
  ): L.LI =
    L.li(
      L.cls(Styles.row),
      DeleteButton(id, metadata.name, storage, toastPublisher, modalController).amend(
        L.cls(Styles.button, Styles.deleteButton)
      ),
      PlanDescriptor(metadata).amend(L.cls(Styles.descriptor)),
      DownloadButton(id, metadata.name, storage, toastPublisher).amend(
        L.cls(Styles.button, Styles.downloadButton)
      ),
      LoadButton(id, storage, selectionObserver, toastPublisher).amend(
        L.cls(Styles.button, Styles.loadButton)
      )
    )
}
