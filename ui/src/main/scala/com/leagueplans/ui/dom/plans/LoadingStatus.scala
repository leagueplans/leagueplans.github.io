package com.leagueplans.ui.dom.plans

import com.leagueplans.ui.dom.plans.PlanManager.State
import com.leagueplans.ui.storage.model.LoadProgress
import com.leagueplans.uicommon.dom.LoadingIcon
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.L

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** A card saying what's being read while a plan loads: the plan list, a plan, or the game data */
object LoadingStatus {
  /** @param gameDataLoaded whether the game data has been read, which plans can't be shown without */
  def apply(state: Signal[State], gameDataLoaded: Signal[Boolean]): L.Div =
    L.div(
      L.cls(Styles.card),
      L.role("status"),
      LoadingIcon().amend(L.svg.cls(Styles.icon)),
      L.div(
        L.cls(Styles.text),
        L.p(L.cls(Styles.title), L.text <-- Signal.combine(state, gameDataLoaded).map(describeState)),
        L.p(
          L.cls(Styles.detail),
          L.text <-- state.flatMapSwitch {
            case State.Opening(_, _, progress) => progress.map(_.map(describe).getOrElse(""))
            case State.Updating(_, _) =>
              Signal.fromValue("It was made with an older version of the site. This can take a few seconds.")
            case _ => Signal.fromValue("")
          }
        )
      )
    )

  @js.native @JSImport("/styles/plans/loadingStatus.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val card: String = js.native
    val icon: String = js.native
    val text: String = js.native
    val title: String = js.native
    val detail: String = js.native
  }

  private def describeState(state: State, gameDataLoaded: Boolean): String =
    state match {
      case State.Starting => "Finding your plans…"
      case State.Opening(_, name, _) => s"Opening “$name”…"
      case State.Updating(_, name) => s"Bringing “$name” up to date…"
      case State.Open(plan, _) if !gameDataLoaded => s"Loading game data for “${plan.name}”…"
      case _ => "Loading…"
    }

  private def describe(progress: LoadProgress): String =
    if (progress.stepsRead < progress.totalSteps)
      s"Read ${progress.stepsRead} of ${progress.totalSteps} steps"
    else
      s"Read all ${progress.totalSteps} steps. Putting the plan together…"
}
