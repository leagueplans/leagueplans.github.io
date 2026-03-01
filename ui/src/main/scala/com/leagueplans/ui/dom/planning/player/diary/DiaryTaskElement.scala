package com.leagueplans.ui.dom.planning.player.diary

import com.leagueplans.ui.dom.common.{ContextMenu, Tooltip}
import com.leagueplans.ui.facades.floatingui.Placement
import com.leagueplans.ui.model.plan.Effect.CompleteDiaryTask
import com.leagueplans.ui.model.player.diary.DiaryTask
import com.leagueplans.ui.wrappers.floatingui.FloatingConfig
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, StringValueMapper, textToTextNode}
import com.raquo.laminar.modifiers.Binder

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object DiaryTaskElement {
  def apply(
    task: DiaryTask,
    completeSignal: Signal[Boolean],
    effectObserverSignal: Signal[Option[Observer[CompleteDiaryTask]]],
    tooltip: Tooltip,
    contextMenuController: ContextMenu.Controller
  ): L.Div =
    L.div(
      L.cls(Styles.element),
      DiaryTierIcon(task.region, task.tier, complete = false).amend(
        L.cls(Styles.icon),
        tooltip.register(
          L.span(L.cls(Styles.tooltip), s"${task.tier} ${task.region.name}"),
          FloatingConfig.basicTooltip(Placement.left)
        )
      ),
      L.span(
        L.cls <-- completeSignal.map {
          case false => ColourStyles.notStarted
          case true => ColourStyles.completed
        },
        L.cls(Styles.description),
        task.description,
      ),
      bindContextMenu(task, completeSignal, effectObserverSignal, contextMenuController)
    )

  @js.native @JSImport("/styles/planning/shared/player/statusColours.module.css", JSImport.Default)
  private object ColourStyles extends js.Object {
    val notStarted: String = js.native
    val completed: String = js.native
  }

  @js.native @JSImport("/styles/planning/player/diary/diaryTaskElement.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val element: String = js.native
    val icon: String = js.native
    val description: String = js.native
    val tooltip: String = js.native
  }

  private def bindContextMenu(
    task: DiaryTask,
    completeSignal: Signal[Boolean],
    effectObserverSignal: Signal[Option[Observer[CompleteDiaryTask]]],
    contextMenuController: ContextMenu.Controller
  ): Binder.Base =
    contextMenuController.register(
      Signal
        .combine(completeSignal, effectObserverSignal)
        .map {
          case (false, Some(effectObserver)) =>
            Some(DiaryTaskContextMenu(task, effectObserver, contextMenuController))
          case _ =>
            None
        }
    )
}
