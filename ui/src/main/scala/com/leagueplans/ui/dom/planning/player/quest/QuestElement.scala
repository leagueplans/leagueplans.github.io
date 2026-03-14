package com.leagueplans.ui.dom.planning.player.quest

import com.leagueplans.ui.dom.common.ContextMenu
import com.leagueplans.ui.model.plan.Effect.CompleteQuest
import com.leagueplans.ui.model.player.Quest
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, StringValueMapper, textToTextNode}
import com.raquo.laminar.modifiers.Binder

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object QuestElement {
  def apply(
    quest: Quest,
    completeSignal: Signal[Boolean],
    effectObserverSignal: Signal[Option[Observer[CompleteQuest]]],
    contextMenu: ContextMenu
  ): L.Div =
    L.div(
      L.cls <-- completeSignal.map {
        case false => ColourStyles.notStarted
        case true => ColourStyles.completed
      },
      quest.name,
      bindContextMenu(quest, completeSignal, effectObserverSignal, contextMenu)
    )

  @js.native @JSImport("/styles/planning/shared/player/statusColours.module.css", JSImport.Default)
  private object ColourStyles extends js.Object {
    val notStarted: String = js.native
    val completed: String = js.native
  }

  private def bindContextMenu(
    quest: Quest,
    completeSignal: Signal[Boolean],
    effectObserverSignal: Signal[Option[Observer[CompleteQuest]]],
    contextMenu: ContextMenu
  ): Binder.Base =
    contextMenu.registerConditionally(
      Signal
        .combine(completeSignal, effectObserverSignal)
        .map {
          case (false, Some(effectObserver)) =>
            Some(() => QuestContextMenu(quest, effectObserver, contextMenu))
          case _ =>
            None
        }
    )()
}
