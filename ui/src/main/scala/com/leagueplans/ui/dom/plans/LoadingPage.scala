package com.leagueplans.ui.dom.plans

import com.leagueplans.ui.dom.plans.PlanManager.State
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.{L, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** Shown while the plan list, a plan or the game data is being read, saying which */
object LoadingPage {
  /** @param state what the plan manager is doing
    * @param gameDataLoaded whether the game data has been read, which plans can't be shown without
    */
  def apply(state: Signal[State], gameDataLoaded: Signal[Boolean]): L.Div =
    L.div(
      L.cls(Styles.page),
      // While the plans are first being found, this matches index.html's message, which it
      // replaces, so that the page doesn't change until there's something new to say
      L.child <-- state.map(_ == State.Starting).distinct.map(isStarting =>
        if (isStarting) L.p(L.cls(Styles.startup), L.role("status"), "Loading the planner…")
        else LoadingStatus(state, gameDataLoaded)
      )
    )

  @js.native @JSImport("/styles/plans/loadingPage.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val page: String = js.native
    val startup: String = js.native
  }
}
