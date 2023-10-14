package ddm.ui.dom.player.quest

import com.raquo.airstream.core.Observer
import com.raquo.laminar.api.{L, eventPropToProcessor, textToNode}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import ddm.ui.dom.common.ContextMenu
import ddm.ui.model.plan.Effect.CompleteQuest
import ddm.ui.model.player.Quest
import ddm.ui.utils.laminar.LaminarOps.RichL
import org.scalajs.dom.MouseEvent
import org.scalajs.dom.html.Button

object QuestContextMenu {
  def apply(
    quest: Quest,
    effectObserver: Observer[CompleteQuest],
    menuCloser: Observer[ContextMenu.CloseCommand]
  ): ReactiveHtmlElement[Button] =
    L.button(
      L.`type`("button"),
      "Complete",
      L.ifUnhandled(L.onClick) -->
        Observer
          .combine(effectObserver, menuCloser)
          .contramap[MouseEvent] { event =>
            event.preventDefault()
            CompleteQuest(quest.id)
          }
    )
}
