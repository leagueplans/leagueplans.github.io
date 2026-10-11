package com.leagueplans.ui.dom

import com.leagueplans.ui.dom.footer.Footer
import com.leagueplans.ui.dom.landing.LandingPage
import com.leagueplans.ui.dom.planning.PlanningPageBootstrap
import com.leagueplans.ui.dom.plans.form.{ImportPlanDialog, NewPlanDialog}
import com.leagueplans.ui.dom.plans.{LoadingPage, PlanManager}
import com.leagueplans.ui.model.player.Cache
import com.leagueplans.ui.model.status.StatusTracker
import com.leagueplans.ui.storage.client.{PlanSubscription, StorageClient}
import com.leagueplans.uicommon.dom.{ContextMenu, Modal, Popover, ToastHub, Tooltip}
import com.leagueplans.uicommon.utils.laminar.HtmlElementOps.trackHeight
import com.raquo.airstream.core.{EventStream, Observer, Signal}
import com.raquo.laminar.api.{L, enrichSource, textToTextNode}
import org.scalajs.dom.window

import scala.concurrent.duration.Duration
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object Bootstrap {
  def apply(): L.Div = {
    val statusTracker = StatusTracker.empty
    val (tooltipContainer, tooltipController) = Tooltip()
    val (contextMenu, contextMenuController) = ContextMenu()
    val (popover, popoverController) = Popover()
    val (toastHub, toastPublisher) = ToastHub(tooltipController)

    val popovers = L.div(
      L.cls(Styles.popovers),
      tooltipContainer.amend(L.cls(Styles.tooltip)),
      contextMenu.amend(L.cls(Styles.contextMenu)),
      popover.amend(L.cls(Styles.popover)),
      toastHub.amend(L.cls(Styles.toastHub))
    )

    val (modalElement, modalController) = Modal(popovers)
    val footer = Footer(statusTracker, tooltipController).amend(L.cls(Styles.footer))
    val manager = PlanManager(StorageClient(), toastPublisher)
    manager.start()

    L.div(
      L.cls(Styles.bootstrap),
      // The footer's credits wrap in narrow windows, so the toasts need to know how tall it is
      // to stay clear of its status indicators
      L.styleProp[String]("--footer-height") <-- footer.trackHeight().map(height => s"${height}px"),
      L.child.maybe <-- modalController.isOpen.invert.map(Option.when(_)(popovers)),
      modalElement.amend(L.cls(Styles.modal)),
      manager.status --> Observer(statusTracker.set(StorageClient.statusKey, _)),
      manager.hasUnsavedChanges --> Observer(warnBeforeClosing),
      L.div(
        L.cls(Styles.page),
        L.child <-- toPageSignal(
          manager, statusTracker, tooltipController, contextMenuController, popoverController, modalController, toastPublisher
        ).map(_.amend(L.cls(Styles.pageContent))),
        footer
      )
    )
  }

  @js.native @JSImport("/styles/bootstrap.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val bootstrap: String = js.native

    val popovers: String = js.native
    val tooltip: String = js.native
    val contextMenu: String = js.native
    val popover: String = js.native
    val toastHub: String = js.native
    val modal: String = js.native

    val page: String = js.native
    val pageContent: String = js.native
    val footer: String = js.native
  }

  /** What the page shows: the planning page once a plan is open and the game data has loaded.
    * While another plan opens, the plan that was open stays on the page, locked, rather than
    * blinking out and back in.
    */
  private enum PageKey {
    case Loading
    case Planning(subscription: PlanSubscription)
    case Picker(notice: Option[PlanManager.Notice])
    case Welcome
  }

  private def toPageSignal(
    manager: PlanManager,
    statusTracker: StatusTracker,
    tooltipController: Tooltip,
    contextMenu: ContextMenu,
    popover: Popover,
    modal: Modal,
    toastPublisher: ToastHub.Publisher
  ): Signal[L.Div] = {
    val onNewPlan = () => NewPlanDialog.open(manager, modal)
    val onImport = () => ImportPlanDialog.open(manager, modal)

    // Read alongside the plans, rather than after them
    val cache = EventStream.fromJsPromise(loadCache(toastPublisher)).map(Some(_)).toSignal(None)
    val gameDataLoaded = cache.map(_.nonEmpty).distinct

    Signal.combine(manager.state, cache)
      .scanLeft(next => (toPageKey(None, next), next))((previous, next) => (toPageKey(Some(previous._1), next), next))
      .splitOne((key, _) => key) {
      case (PageKey.Planning(subscription), (_, (PlanManager.State.Open(plan, _), Some(cache))), _) =>
        PlanningPageBootstrap(
          manager,
          plan,
          subscription,
          statusTracker,
          cache,
          tooltipController,
          contextMenu,
          popover,
          modal,
          toastPublisher,
          onNewPlan,
          onImport
        )

      // Until the plan picker and the welcome screen are built, the landing page stands in for both
      case (key, _, _) if key != PageKey.Loading =>
        val landingPage = LandingPage(Observer(manager.adopt), manager.storage, tooltipController, toastPublisher, modal)
        key match {
          case PageKey.Picker(Some(PlanManager.Notice.Info(text))) =>
            landingPage.amend(L.p(L.role("status"), text))
          case PageKey.Picker(Some(PlanManager.Notice.Problem(title, detail))) =>
            landingPage.amend(L.p(L.role("alert"), s"$title: $detail"))
          case _ =>
            landingPage
        }

      case (_, _, signal) =>
        LoadingPage(manager, signal.map { case (_, (state, _)) => state }, gameDataLoaded, popover, tooltipController, onNewPlan, onImport)
    }
  }

  private def toPageKey(previous: Option[PageKey], next: (PlanManager.State, Option[Cache])): PageKey =
    next match {
      case (PlanManager.State.Open(_, subscription), Some(_)) => PageKey.Planning(subscription)
      case (PlanManager.State.Picker(notice), _) => PageKey.Picker(notice)
      case (PlanManager.State.Welcome, _) => PageKey.Welcome
      case _ =>
        previous match {
          case Some(planning: PageKey.Planning) => planning
          case _ => PageKey.Loading
        }
    }

  /** Asks the browser to warn before the tab is closed while changes are being saved */
  private def warnBeforeClosing(hasUnsavedChanges: Boolean): Unit =
    window.onbeforeunload =
      if (hasUnsavedChanges) _.preventDefault()
      else _ => ()

  private def loadCache(toastPublisher: ToastHub.Publisher): js.Promise[Cache] =
    js.async(
      js.await(Cache.load()) match {
        case Right(cache) => cache
        case Left((key, error)) =>
          toastPublisher.publish(
            ToastHub.Type.Error,
            Duration.Inf,
            "Couldn't load game data",
            Some(s"Failed to load $key")
          )
          throw error
      }
    )
}
