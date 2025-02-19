package com.leagueplans.ui.dom.player.diary

import com.leagueplans.ui.dom.common.{Button, ContextMenu}
import com.leagueplans.ui.model.plan.Effect.CompleteDiaryTask
import com.leagueplans.ui.model.player.diary.DiaryTask
import com.leagueplans.ui.utils.laminar.EventProcessorOps.handledAs
import com.raquo.airstream.core.Observer
import com.raquo.laminar.api.{L, textToTextNode}

object DiaryTaskContextMenu {
  def apply(
    diaryTask: DiaryTask,
    effectObserver: Observer[CompleteDiaryTask],
    menuCloser: Observer[ContextMenu.CloseCommand]
  ): L.Button =
    Button(
      _.handledAs[CompleteDiaryTask](CompleteDiaryTask(diaryTask.id)) --> 
        Observer.combine(effectObserver, menuCloser)
    ).amend("Complete")
}
