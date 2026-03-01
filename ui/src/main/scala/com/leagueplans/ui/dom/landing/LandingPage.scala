package com.leagueplans.ui.dom.landing

import com.leagueplans.ui.dom.common.{LoadingIcon, Modal, ToastHub, Tooltip}
import com.leagueplans.ui.dom.landing.changelog.Changelog
import com.leagueplans.ui.dom.landing.form.NewPlanForm
import com.leagueplans.ui.dom.landing.menu.PlansMenu
import com.leagueplans.ui.model.plan.Plan
import com.leagueplans.ui.storage.client.{PlanSubscription, StorageClient}
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, enrichSource, textToTextNode}

import scala.concurrent.duration.DurationInt
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object LandingPage {
  def apply(
    planObserver: Observer[(Plan, PlanSubscription)],
    tooltip: Tooltip,
    toastPublisher: ToastHub.Publisher,
    modal: Modal
  ): L.Div =
    L.div(
      L.cls(Styles.page),
      changeLog(),
      plansForm(planObserver, tooltip, toastPublisher, modal)
    )

  @js.native @JSImport("/styles/landing/landingPage.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val page: String = js.native
    val content: String = js.native
    val intro: String = js.native
    val loadingIcon: String = js.native
    val menu: String = js.native
    val menuContent: String = js.native
    val disclaimer: String = js.native
  }
  
  private def changeLog(): L.Div =
    L.div(
      L.cls(Styles.content),
      L.p(L.cls(Styles.intro), "Recent changes"),
      Changelog()
    )
  
  private def plansForm(
    planObserver: Observer[(Plan, PlanSubscription)],
    tooltip: Tooltip,
    toastPublisher: ToastHub.Publisher,
    modal: Modal
  ): L.Div = {
    val storage = StorageClient()
    val newPlanForm = NewPlanForm(storage, planObserver, tooltip, toastPublisher)

    L.div(
      L.cls(Styles.content),
      L.p(L.cls(Styles.intro), "Create a new plan"),
      newPlanForm,
      L.child <-- toExistingPlans(storage, planObserver, tooltip, toastPublisher, modal),
      L.p(
        L.cls(Styles.disclaimer),
        "Plans are saved against your browser's local storage. As a result, wiping your" +
          " browser's storage will delete your plans. No data is saved remotely."
      )
    )
  }

  private def toExistingPlans(
    storage: StorageClient,
    planObserver: Observer[(Plan, PlanSubscription)],
    tooltip: Tooltip,
    toastPublisher: ToastHub.Publisher,
    modal: Modal
  ): Signal[L.Node] = {
    val hasRefreshedState = Var(false)
    storage
      .plansSignal
      .combineWith(hasRefreshedState)
      .splitOne((plans, hasRefreshed) => (hasRefreshed, plans.isEmpty)) {
        case ((false, true), _, _) => toLoadingNode(storage, toastPublisher, hasRefreshedState)
        case ((true, true), _, _) => L.emptyNode
        case ((_, false), _, _) => toMenu(storage, planObserver, tooltip, toastPublisher, modal)
      }
  }

  private def toLoadingNode(
    storage: StorageClient,
    toastPublisher: ToastHub.Publisher,
    hasRefreshedState: Var[Boolean]
  ): L.Div =
    L.div(
      L.cls(Styles.menu),
      L.p(L.cls(Styles.intro), "Checking for existing plans..."),
      LoadingIcon().amend(L.svg.cls(Styles.loadingIcon)),
      storage.refreshPlans().changes.collectSome --> {
        case Left(error) =>
          toastPublisher.publish(
            ToastHub.Type.Warning,
            15.seconds,
            s"Failed to load plan. Cause: [${error.message}]"
          )
        case Right(()) =>
          hasRefreshedState.set(true)
      }
    )

  def toMenu(
    storage: StorageClient,
    planObserver: Observer[(Plan, PlanSubscription)],
    tooltip: Tooltip,
    toastPublisher: ToastHub.Publisher,
    modal: Modal
  ): L.Div =
    L.div(
      L.cls(Styles.menu),
      L.p(L.cls(Styles.intro), "Or load an existing plan"),
      PlansMenu(
        storage,
        planObserver,
        tooltip,
        toastPublisher,
        modal
      ).amend(L.cls(Styles.menuContent))
    )
}
