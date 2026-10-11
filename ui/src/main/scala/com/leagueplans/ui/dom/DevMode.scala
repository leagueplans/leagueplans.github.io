package com.leagueplans.ui.dom

import com.leagueplans.ui.dom.plans.PlanManager.{Notice, State}
import com.leagueplans.ui.storage.model.{LoadProgress, PlanID}
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.{Val, Var}
import com.raquo.laminar.api.{L, enrichSource}
import org.scalajs.dom.{URLSearchParams, window}

/** Conveniences for working on the site, offered only when it's served from this machine */
object DevMode {
  val isOn: Boolean =
    window.location.hostname == "localhost"

  /** Pages that the site only shows in particular circumstances, such as to new visitors or after
    * a plan is deleted, which are easy to forget otherwise. Each can be shown on request, whatever
    * plans this browser has, until the user does something that opens a plan. */
  enum Page(val key: String, val label: String) {
    case Welcome extends Page("welcome", "Welcome screen")
    case Picker extends Page("picker", "Plan picker")
    case PickerAfterDelete extends Page("picker-deleted", "Plan picker, after a delete")
    case PickerAfterProblem extends Page("picker-problem", "Plan picker, after a problem")
    case Startup extends Page("startup", "Loading, at startup")
    case Opening extends Page("opening", "Loading, opening a plan")
  }

  private val param = "dev-page"

  def url(page: Page): String =
    s"/?$param=${page.key}"

  /** Shows the page asked for in the address, if there is one, in place of the plan manager's. It
    * shows until the user does something that opens a plan: [[leave]] is called, or the manager's
    * state changes once startup has settled.
    */
  final class PageOverride(managerState: Signal[State]) {
    private val requested = Var(requestedPage)
    // The manager's state changes by itself until startup settles, and after that only when the
    // user asks
    private var hasSettled = false

    /** What the page shows */
    val state: Signal[State] =
      managerState.combineWith(requested.signal).map((state, page) => page.fold(state)(stateFor))

    def leave(): Unit =
      if (requested.now().nonEmpty) {
        requested.set(None)
        forgetRequestedPage()
      }

    /** Watches the manager for the user opening a plan */
    val binder: L.Modifier[L.Element] =
      managerState --> Observer[State] {
        case _ if hasSettled => leave()
        case State.Open(_, _) | State.Picker(_) | State.Welcome => hasSettled = true
        case _ => ()
      }
  }

  private def requestedPage: Option[Page] =
    if (!isOn) None
    else Option(URLSearchParams(window.location.search).get(param)).flatMap(key => Page.values.find(_.key == key))

  /** Takes the request out of the address, so that it doesn't apply again after a reload */
  private def forgetRequestedPage(): Unit =
    window.history.replaceState(null, "", window.location.pathname)

  /** The plan manager's state that shows the page */
  private def stateFor(page: Page): State =
    page match {
      case Page.Welcome => State.Welcome
      case Page.Picker => State.Picker(None)
      case Page.PickerAfterDelete => State.Picker(Some(Notice.Info("Deleted “Example plan”.")))
      case Page.PickerAfterProblem =>
        State.Picker(Some(Notice.Problem("Couldn't open “Example plan”", "This is an example problem, shown for development.")))
      case Page.Startup => State.Starting
      case Page.Opening =>
        State.Opening(PlanID.fromString("dev-example-plan"), "Example plan", Val(Some(LoadProgress(340, 1455))))
    }
}
