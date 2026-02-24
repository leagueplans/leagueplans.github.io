package com.leagueplans.ui.dom.planning.player.quest

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
    controller: ContextMenu.Controller
  ): L.Button =
    Button(
      _.handledAs[CompleteQuest](CompleteQuest(quest.id)) --> 
        Observer.combine(effectObserver, Observer(_ => controller.close()))
    ).amend("Complete")
}
