package ddm.ui.dom.plans

import com.raquo.airstream.core.Observer
import com.raquo.airstream.eventbus.WriteBus
import com.raquo.laminar.api.{L, StringSeqValueMapper, textToTextNode}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import ddm.ui.PlanStorage
import ddm.ui.facades.fontawesome.freesolid.FreeSolid
import ddm.ui.utils.laminar.FontAwesome
import ddm.ui.utils.laminar.LaminarOps.*
import org.scalajs.dom.{Blob, BlobPropertyBag, HTMLOListElement, URL}

import scala.scalajs.js
import scala.scalajs.js.JSConverters.JSRichIterable
import scala.scalajs.js.annotation.JSImport

object PlansList {
  def apply(
    planStorage: PlanStorage,
    loadObserver: Observer[PlanStorage.Result],
    modalBus: WriteBus[Option[L.Element]]
  ): ReactiveHtmlElement[HTMLOListElement] =
    L.ol(
      L.cls(Styles.list),
      L.children <--
        planStorage
          .plansSignal
          .map(_.toList.sorted)
          .split(identity)((_, planName, _) =>
            toNode(planName, planStorage, loadObserver, modalBus)
          )
    )

  @js.native @JSImport("/styles/plans/plansList.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val list: String = js.native
    val row: String = js.native
    val name: String = js.native
    val button: String = js.native
    val downloadButton: String = js.native
    val deleteButton: String = js.native
    val loadButton: String = js.native
  }

  private def toNode(
    planName: String,
    planStorage: PlanStorage,
    loadObserver: Observer[PlanStorage.Result],
    modalBus: WriteBus[Option[L.Element]]
  ): L.LI =
    L.li(
      L.cls(Styles.row),
      deleteButton(planName, planStorage, modalBus),
      L.span(L.cls(Styles.name), planName),
      downloadButton(planName, planStorage),
      loadButton(planName, planStorage, loadObserver)
    )

  private def downloadButton(
    planName: String,
    planStorage: PlanStorage
  ): L.Button =
    L.button(
      L.cls(Styles.button, Styles.downloadButton),
      L.`type`("button"),
      FontAwesome.icon(FreeSolid.faDownload),
      L.onClick.handled --> Observer[Unit](_ => triggerDownload(planName, planStorage))
    )

  private def triggerDownload(planName: String, planStorage: PlanStorage): Unit = {
    val url = createDownloadURL(planName, planStorage)
    L.a(L.href(url), L.download(s"$planName.json")).ref.click()
    URL.revokeObjectURL(url)
  }

  private def createDownloadURL(planName: String, planStorage: PlanStorage): String = {
    val data = planStorage.rawPlanData(planName).getOrElse("")
    val blob = Blob(
      List(data).toJSIterable,
      new BlobPropertyBag { `type` = "application/json" }
    )
    URL.createObjectURL(blob)
  }

  private def deleteButton(
    planName: String,
    planStorage: PlanStorage,
    modalBus: WriteBus[Option[L.Element]]
  ): L.Button = {
    val deletionConfirmer = DeletionConfirmer(
      modalBus,
      Observer(_ => planStorage.deletePlan(planName))
    )

    L.button(
      L.cls(Styles.button, Styles.deleteButton),
      L.`type`("button"),
      FontAwesome.icon(FreeSolid.faXmark),
      L.onClick.handled --> deletionConfirmer
    )
  }

  private def loadButton(
    planName: String,
    planStorage: PlanStorage,
    loadObserver: Observer[PlanStorage.Result]
  ): L.Button =
    L.button(
      L.cls(Styles.button, Styles.loadButton),
      L.`type`("button"),
      "Load",
      L.onClick.handledAs(planStorage.loadPlan(planName)) --> loadObserver
    )
}
