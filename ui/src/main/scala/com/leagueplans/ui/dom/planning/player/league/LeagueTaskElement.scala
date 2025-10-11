package com.leagueplans.ui.dom.planning.player.league

import com.leagueplans.common.model.LeagueTask
import com.leagueplans.ui.dom.common.ContextMenu
import com.leagueplans.ui.model.plan.Effect.CompleteLeagueTask
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, StringValueMapper, textToTextNode}
import com.raquo.laminar.modifiers.Binder

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object LeagueTaskElement {
  def apply(
    task: LeagueTask,
    completeSignal: Signal[Boolean],
    effectObserverSignal: Signal[Option[Observer[CompleteLeagueTask]]],
    contextMenuController: ContextMenu.Controller
  ): L.Div =
    L.div(
      L.cls(Styles.element),
      L.p(
        L.cls(Styles.name),
        L.cls <-- completeSignal.map {
          case false => ColourStyles.notStarted
          case true => ColourStyles.completed
        },
        task.name
      ),
      L.p(L.cls(Styles.description), task.description),
      bindContextMenu(task, completeSignal, effectObserverSignal, contextMenuController)
    )

  @js.native @JSImport("/styles/planning/shared/player/statusColours.module.css", JSImport.Default)
  private object ColourStyles extends js.Object {
    val notStarted: String = js.native
    val completed: String = js.native
  }

  @js.native @JSImport("/styles/planning/player/league/leagueTaskElement.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val element: String = js.native
    val name: String = js.native
    val description: String = js.native
  }

  private def bindContextMenu(
    task: LeagueTask,
    completeSignal: Signal[Boolean],
    effectObserverSignal: Signal[Option[Observer[CompleteLeagueTask]]],
    contextMenuController: ContextMenu.Controller
  ): Binder.Base =
    contextMenuController.bind(menuCloser =>
      Signal
        .combine(completeSignal, effectObserverSignal)
        .map {
          case (false, Some(effectObserver)) =>
            Some(LeagueTaskContextMenu(task, effectObserver, menuCloser))
          case _ =>
            None
        }
    )
}
