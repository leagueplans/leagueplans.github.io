package ddm.ui.dom.player.diary

import com.raquo.airstream.core.Observer
import com.raquo.laminar.api.{L, textToTextNode}
import ddm.ui.dom.common.{Button, ContextMenu}
import ddm.ui.model.plan.Effect.CompleteDiaryTask
import ddm.ui.model.player.diary.DiaryTask
import ddm.ui.utils.laminar.LaminarOps.handledAs

object DiaryTaskContextMenu {
  def apply(
    diaryTask: DiaryTask,
    effectObserver: Observer[CompleteDiaryTask],
    menuCloser: Observer[ContextMenu.CloseCommand]
  ): L.Button =
    Button(
      Observer.combine(effectObserver, menuCloser)
    )(_.handledAs(CompleteDiaryTask(diaryTask.id))).amend("Complete")
}
