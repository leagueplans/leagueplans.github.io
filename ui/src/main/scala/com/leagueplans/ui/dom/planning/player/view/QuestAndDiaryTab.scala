package com.leagueplans.ui.dom.planning.player.view

import com.leagueplans.ui.dom.common.ContextMenu
import com.leagueplans.ui.dom.planning.player.diary.DiaryPanel
import com.leagueplans.ui.dom.planning.player.quest.QuestList
import com.leagueplans.ui.model.plan.Effect
import com.leagueplans.ui.model.player.{Cache, Player}
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.L

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object QuestAndDiaryTab {
  def apply(
    playerSignal: Signal[Player],
    cache: Cache,
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    contextMenuController: ContextMenu.Controller
  ): L.Div =
    L.div(
      L.cls(Styles.tabContent),
      QuestList(playerSignal, cache, effectObserverSignal, contextMenuController).amend(L.cls(Styles.questPanel)),
      DiaryPanel(playerSignal, cache, effectObserverSignal, contextMenuController).amend(L.cls(Styles.diaryPanel))
    )

  @js.native @JSImport("/styles/planning/player/view/questAndDiaryTab.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val tabContent: String = js.native
    val questPanel: String = js.native
    val diaryPanel: String = js.native
  }
}
