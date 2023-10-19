package ddm.ui.dom.player.quest

import com.raquo.airstream.core.Observer
import com.raquo.laminar.api.{L, textToTextNode}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import ddm.ui.dom.common.ContextMenu
import ddm.ui.model.plan.Effect.CompleteQuest
import ddm.ui.model.player.Quest
import ddm.ui.utils.laminar.LaminarOps.RichEventProp
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
      L.onClick.handledAs(CompleteQuest(quest.id)) -->
        Observer.combine(effectObserver, menuCloser)
    )
}
