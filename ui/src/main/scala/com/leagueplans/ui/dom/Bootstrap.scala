package com.leagueplans.ui.dom

import com.leagueplans.ui.dom.common.{ContextMenu, Modal, ToastHub, Tooltip}
import com.leagueplans.ui.dom.footer.Footer
import com.leagueplans.ui.dom.landing.LandingPage
import com.leagueplans.ui.dom.planning.PlanningPageBootstrap
import com.leagueplans.ui.model.plan.Plan
import com.leagueplans.ui.model.player.Cache
import com.leagueplans.ui.storage.client.PlanSubscription
import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.laminar.api.{L, textToTextNode}

import scala.concurrent.duration.Duration
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object Bootstrap {
  def apply(): L.Div = {
    val (tooltipContainer, tooltipController) = Tooltip()
    val (contextMenu, contextMenuController) = ContextMenu()
    val (toastHub, toastPublisher) = ToastHub(tooltipController)

    val popovers = L.div(
      L.cls(Styles.popovers),
      tooltipContainer.amend(L.cls(Styles.tooltip)),
      contextMenu.amend(L.cls(Styles.contextMenu)),
      toastHub.amend(L.cls(Styles.toastHub))
    )

    val (modalElement, modalController) = Modal(popovers)

    L.div(
      L.cls(Styles.bootstrap),
      L.child.maybe <-- modalController.isOpen.invert.map(Option.when(_)(popovers)),
      modalElement.amend(L.cls(Styles.modal)),
      L.div(
        L.cls(Styles.page),
        L.child <-- toPageSignal(
          tooltipController, contextMenuController, modalController, toastPublisher
        ).map(_.amend(L.cls(Styles.pageContent))),
        Footer().amend(L.cls(Styles.footer))
      )
    )
  }

  @js.native @JSImport("/styles/bootstrap.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val bootstrap: String = js.native

    val popovers: String = js.native
    val tooltip: String = js.native
    val contextMenu: String = js.native
    val toastHub: String = js.native
    val modal: String = js.native

    val page: String = js.native
    val pageContent: String = js.native
    val footer: String = js.native
  }

  private def toPageSignal(
    tooltipController: Tooltip,
    contextMenuController: ContextMenu.Controller,
    modal: Modal,
    toastPublisher: ToastHub.Publisher
  ): Signal[L.Div] = {
    val planBus = EventBus[(Plan, PlanSubscription)]()
    val cacheLoader = EventStream.fromJsPromise(loadCache(toastPublisher))

    planBus.events.combineWith(cacheLoader).map((plan, subscription, cache) =>
      PlanningPageBootstrap(
        plan,
        subscription,
        cache,
        tooltipController,
        contextMenuController,
        modal,
        toastPublisher
      )
    ).toSignal(initial = LandingPage(planBus.writer, tooltipController, toastPublisher, modal))
  }

  private def loadCache(toastPublisher: ToastHub.Publisher): js.Promise[Cache] =
    js.async(
      js.await(Cache.load()) match {
        case Right(cache) => cache
        case Left((key, error)) =>
          toastPublisher.publish(
            ToastHub.Type.Error,
            Duration.Inf,
            s"Failed to load $key"
          )
          throw error
      }
    )
}
