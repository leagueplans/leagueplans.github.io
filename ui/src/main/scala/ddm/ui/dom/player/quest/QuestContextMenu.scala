package ddm.ui.dom.player.quest

import com.raquo.airstream.core.Observer
import com.raquo.laminar.api.{L, textToTextNode}
import ddm.ui.dom.common.{Button, ContextMenu}
import ddm.ui.model.plan.Effect.CompleteQuest
import ddm.ui.model.player.Quest
import ddm.ui.utils.laminar.LaminarOps.handledAs

object QuestContextMenu {
  def apply(
    quest: Quest,
    effectObserver: Observer[CompleteQuest],
    menuCloser: Observer[ContextMenu.CloseCommand]
  ): L.Button =
    Button(
      Observer.combine(effectObserver, menuCloser)
    )(_.handledAs(CompleteQuest(quest.id))).amend("Complete")
}
