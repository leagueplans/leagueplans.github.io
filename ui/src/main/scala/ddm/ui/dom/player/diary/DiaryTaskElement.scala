package ddm.ui.dom.player.diary

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, StringValueMapper, textToTextNode}
import com.raquo.laminar.modifiers.Binder
import com.raquo.laminar.nodes.ReactiveElement.Base
import ddm.ui.dom.common.{ContextMenu, Tooltip}
import ddm.ui.model.plan.Effect.CompleteDiaryTask
import ddm.ui.model.player.diary.DiaryTask

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object DiaryTaskElement {
  def apply(
    task: DiaryTask,
    completeSignal: Signal[Boolean],
    effectObserverSignal: Signal[Option[Observer[CompleteDiaryTask]]],
    contextMenuController: ContextMenu.Controller
  ): L.Div =
    L.div(
      L.cls(Styles.element),
      DiaryTierIcon(task.region, task.tier, complete = false).amend(
        L.cls(Styles.icon),
        Tooltip(L.span(s"${task.tier} ${task.region.name}"))
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

  @js.native @JSImport("/styles/shared/player/statusColours.module.css", JSImport.Default)
  private object ColourStyles extends js.Object {
    val notStarted: String = js.native
    val completed: String = js.native
  }

  @js.native @JSImport("/styles/player/diary/diaryTaskElement.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val element: String = js.native
    val icon: String = js.native
    val description: String = js.native
  }

  private def bindContextMenu(
    task: DiaryTask,
    completeSignal: Signal[Boolean],
    effectObserverSignal: Signal[Option[Observer[CompleteDiaryTask]]],
    contextMenuController: ContextMenu.Controller
  ): Binder[Base] =
    contextMenuController.bind(menuCloser =>
      Signal
        .combine(completeSignal, effectObserverSignal)
        .map {
          case (false, Some(effectObserver)) =>
            Some(DiaryTaskContextMenu(task, effectObserver, menuCloser))
          case _ =>
            None
        }
    )
}
