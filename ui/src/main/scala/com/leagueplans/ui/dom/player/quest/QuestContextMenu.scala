package com.leagueplans.ui.dom.player.quest

import com.leagueplans.ui.dom.common.{Button, ContextMenu}
import com.leagueplans.ui.model.plan.Effect.CompleteQuest
import com.leagueplans.ui.model.player.Quest
import com.leagueplans.ui.utils.laminar.EventProcessorOps.handledAs
import com.raquo.airstream.core.Observer
import com.raquo.laminar.api.{L, textToTextNode}

object QuestContextMenu {
  def apply(
    quest: Quest,
    effectObserver: Observer[CompleteQuest],
    menuCloser: Observer[ContextMenu.CloseCommand]
  ): L.Button =
    Button(
      _.handledAs[CompleteQuest](CompleteQuest(quest.id)) --> 
        Observer.combine(effectObserver, menuCloser)
    ).amend("Complete")
}
