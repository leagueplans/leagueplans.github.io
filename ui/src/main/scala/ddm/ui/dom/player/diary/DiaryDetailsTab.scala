package ddm.ui.dom.player.diary

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.L
import ddm.ui.dom.common.ContextMenu
import ddm.ui.model.plan.Effect.CompleteDiaryTask
import ddm.ui.model.player.Cache
import ddm.ui.model.player.diary.{DiaryRegion, DiaryTier}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object DiaryDetailsTab {
  def apply(
    completedTasksSignal: Signal[Set[Int]],
    cache: Cache,
    effectObserverSignal: Signal[Option[Observer[CompleteDiaryTask]]],
    contextMenuController: ContextMenu.Controller,
    regionVar: Var[Option[DiaryRegion]],
    tierVar: Var[Option[DiaryTier]]
  ): L.Div = {
    val progressVar = Var(Option.empty[Progress])

    L.div(
      L.cls(Styles.tab),
      DiaryFilters(regionVar, tierVar, progressVar).amend(L.cls(Styles.filters)),
      DiaryTaskList(
        completedTasksSignal,
        cache,
        effectObserverSignal,
        contextMenuController,
        regionVar.signal,
        tierVar.signal,
        progressVar.signal
      ).amend(L.cls(Styles.tasks))
    )
  }

  sealed trait Progress

  object Progress {
    case object Incomplete extends Progress
    case object Complete extends Progress
  }

  @js.native @JSImport("/styles/player/diary/diaryDetailsTab.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val tab: String = js.native
    val tasks: String = js.native
    val filters: String = js.native
  }
}
