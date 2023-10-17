package ddm.ui.dom.player.diary

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.L
import com.raquo.laminar.nodes.ReactiveHtmlElement
import ddm.ui.dom.common.ContextMenu
import ddm.ui.dom.player.diary.DiaryDetailsTab.Progress
import ddm.ui.model.plan.Effect.CompleteDiaryTask
import ddm.ui.model.player.Cache
import ddm.ui.model.player.diary.{DiaryRegion, DiaryTask, DiaryTier}
import org.scalajs.dom.html.OList

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object DiaryTaskList {
  def apply(
    completedTasksSignal: Signal[Set[Int]],
    cache: Cache,
    effectObserverSignal: Signal[Option[Observer[CompleteDiaryTask]]],
    contextMenuController: ContextMenu.Controller,
    regionFilterSignal: Signal[Option[DiaryRegion]],
    tierFilterSignal: Signal[Option[DiaryTier]],
    progressFilterSignal: Signal[Option[DiaryDetailsTab.Progress]]
  ): ReactiveHtmlElement[OList] =
    L.ol(
      L.cls(Styles.list),
      L.children <--
        Signal
          .combine(completedTasksSignal, regionFilterSignal, tierFilterSignal, progressFilterSignal)
          .map { case (completedTasks, regionFilter, tierFilter, progressFilter) =>
            enumerateTasks(cache, completedTasks, regionFilter, tierFilter, progressFilter)
          }
          .map(tasks =>
            tasks.map(task =>
              L.li(
                L.cls(Styles.entry),
                DiaryTaskElement(
                  task,
                  completedTasksSignal.map(_.contains(task.id)),
                  effectObserverSignal,
                  contextMenuController
                )
              )
            )
          )
    )

  @js.native @JSImport("/styles/player/diary/diaryTaskList.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val list: String = js.native
    val entry: String = js.native
  }

  private def enumerateTasks(
    cache: Cache,
    completedTasks: Set[Int],
    regionFilter: Option[DiaryRegion],
    tierFilter: Option[DiaryTier],
    progressFilter: Option[Progress]
  ): List[DiaryTask] =
    cache
      .diaryTasks
      .values
      .filter(task =>
        regionFilter.forall(_ == task.region) &&
          tierFilter.forall(_ == task.tier) &&
          progressFilter.forall {
            case Progress.Incomplete => !completedTasks.contains(task.id)
            case Progress.Complete => completedTasks.contains(task.id)
          }
      )
      .toList
      .sortBy(task => (task.tier, task.region, task.id))
}
