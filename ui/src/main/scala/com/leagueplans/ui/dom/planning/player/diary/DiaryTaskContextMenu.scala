package com.leagueplans.ui.dom.planning.player.diary

import com.leagueplans.ui.model.plan.Effect.CompleteDiaryTask
import com.leagueplans.ui.model.player.diary.DiaryTask
import com.leagueplans.uicommon.dom.{Button, ContextMenu, ContextMenuList}
import com.leagueplans.uicommon.facades.fontawesome.freesolid.FreeSolid
import com.leagueplans.uicommon.utils.laminar.EventProcessorOps.handledAs
import com.leagueplans.uicommon.utils.laminar.FontAwesome
import com.raquo.airstream.core.Observer
import com.raquo.laminar.api.L

object DiaryTaskContextMenu {
  def apply(
    diaryTask: DiaryTask,
    effectObserver: Observer[CompleteDiaryTask],
    contextMenu: ContextMenu
  ): L.Div =
    ContextMenuList(
      ContextMenuList.Item(
        FontAwesome.icon(FreeSolid.faCheck),
        "Complete",
        Button(
          _.handledAs[CompleteDiaryTask](CompleteDiaryTask(diaryTask.id)) -->
            Observer.combine(effectObserver, Observer(_ => contextMenu.close()))
        )
      )
    )
}
