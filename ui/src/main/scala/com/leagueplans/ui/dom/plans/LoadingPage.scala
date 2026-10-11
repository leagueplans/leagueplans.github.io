package com.leagueplans.ui.dom.plans

import com.leagueplans.ui.dom.plans.PlanManager.State
import com.leagueplans.uicommon.dom.{Popover, Tooltip}
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.{L, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** Shown while the plan list, a plan or the game data is being read, when there's no plan already
  * on the page to show meanwhile. Names the plan being opened, with a list of the other plans to
  * pick instead.
  */
object LoadingPage {
  /** @param state what the plan manager is doing
    * @param gameDataLoaded whether the game data has been read, which plans can't be shown without
    * @param onNewPlan called when the user asks to make a new plan, from the plan switcher
    * @param onImport called when the user asks to import a plan file, from the plan switcher
    */
  def apply(
    manager: PlanManager,
    state: Signal[State],
    gameDataLoaded: Signal[Boolean],
    popover: Popover,
    tooltip: Tooltip,
    onNewPlan: () => Unit,
    onImport: () => Unit
  ): L.Div = {
    val plan = state.map(PlanSwitcher.toCurrent)

    L.div(
      L.cls(Styles.page),
      // While the plans are first being found, this matches index.html's message, which it
      // replaces, so that the page doesn't change until there's something new to say
      L.child <-- state.map(_ == State.Starting).distinct.map(isStarting =>
        if (isStarting) L.p(L.cls(Styles.startup), L.role("status"), "Loading the planner…")
        else
          L.div(
            L.cls(Styles.column),
            L.child.maybe <-- plan.map(_.nonEmpty).distinct.map(Option.when(_)(
              PlanSwitcher(manager, plan, popover, tooltip, onNewPlan, onImport)
            )),
            LoadingStatus(state, gameDataLoaded)
          )
      )
    )
  }

  @js.native @JSImport("/styles/plans/loadingPage.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val page: String = js.native
    val column: String = js.native
    val startup: String = js.native
  }
}
