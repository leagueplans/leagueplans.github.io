package ddm.ui.dom.player.quest

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, StringValueMapper, textToNode}
import com.raquo.laminar.modifiers.Binder
import com.raquo.laminar.nodes.ReactiveElement.Base
import ddm.ui.dom.common.ContextMenu
import ddm.ui.model.plan.Effect.CompleteQuest
import ddm.ui.model.player.Quest

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object QuestElement {
  def apply(
    quest: Quest,
    completeSignal: Signal[Boolean],
    effectObserverSignal: Signal[Option[Observer[CompleteQuest]]],
    contextMenuController: ContextMenu.Controller
  ): L.Span =
    L.span(
      L.cls <-- completeSignal.map {
        case false => ColourStyles.notStarted
        case true => ColourStyles.completed
      },
      quest.name,
      bindContextMenu(quest, completeSignal, effectObserverSignal, contextMenuController)
    )

  @js.native @JSImport("/styles/shared/player/statusColours.module.css", JSImport.Default)
  private object ColourStyles extends js.Object {
    val notStarted: String = js.native
    val completed: String = js.native
  }

  private def bindContextMenu(
    quest: Quest,
    completeSignal: Signal[Boolean],
    effectObserverSignal: Signal[Option[Observer[CompleteQuest]]],
    contextMenuController: ContextMenu.Controller
  ): Binder[Base] =
    contextMenuController.bind(menuCloser =>
      Signal
        .combine(completeSignal, effectObserverSignal)
        .map {
          case (false, Some(effectObserver)) =>
            Some(QuestContextMenu(quest, effectObserver, menuCloser))
          case _ =>
            None
        }
    )
}
