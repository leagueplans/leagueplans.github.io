package ddm.ui.dom.player.diary

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, textToTextNode}
import ddm.ui.dom.common.ContextMenu
import ddm.ui.dom.player.task.TaskPanel
import ddm.ui.model.plan.Effect.CompleteDiaryTask
import ddm.ui.model.player.diary.{DiaryRegion, DiaryTier}
import ddm.ui.model.player.{Cache, Player}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object DiaryPanel {
  def apply(
    playerSignal: Signal[Player],
    cache: Cache,
    effectObserverSignal: Signal[Option[Observer[CompleteDiaryTask]]],
    contextMenuController: ContextMenu.Controller
  ): L.Div = {
    val completeTasksSignal = playerSignal.map(_.completedDiaryTasks)
    val regionVar = Var(Option.empty[DiaryRegion])
    val tierVar = Var(Option.empty[DiaryTier])

    val toSummaryTab =
      (toggleObserver: Observer[Unit]) =>
        DiarySummaryTab(
          completeTasksSignal,
          cache,
          Observer.combine(
            toggleObserver.contramap[DiaryRegion](_ => ()),
            regionVar.someWriter
          ),
          Observer.combine(
            toggleObserver.contramap[Option[DiaryTier]](_ => ()),
            tierVar.writer
          )
        )

    TaskPanel(
      L.headerTag(
        L.img(L.cls(Styles.titleIcon), L.src(icon), L.alt("Achievement diary icon")),
        "Achievement diaries"
      ),
      toSummaryTab,
      DiaryDetailsTab(
        completeTasksSignal,
        cache,
        effectObserverSignal,
        contextMenuController,
        regionVar,
        tierVar
      )
    )
  }

  @js.native @JSImport("/images/achievement-diary-icon.png", JSImport.Default)
  private val icon: String = js.native

  @js.native @JSImport("/styles/player/diary/diaryPanel.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val titleIcon: String = js.native
  }
}
