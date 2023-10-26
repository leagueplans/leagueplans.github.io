package ddm.ui.dom.player.view

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.L
import ddm.ui.dom.common.ContextMenu
import ddm.ui.dom.player.diary.DiaryPanel
import ddm.ui.dom.player.quest.QuestList
import ddm.ui.model.plan.Effect
import ddm.ui.model.player.{Cache, Player}

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

  @js.native @JSImport("/styles/player/view/questAndDiaryTab.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val tabContent: String = js.native
    val questPanel: String = js.native
    val diaryPanel: String = js.native
  }
}
