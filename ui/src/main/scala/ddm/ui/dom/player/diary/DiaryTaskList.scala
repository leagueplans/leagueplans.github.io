package ddm.ui.dom.player.diary

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import ddm.ui.dom.common.ContextMenu
import ddm.ui.dom.player.task.{TaskDetailsTab, TaskList}
import ddm.ui.model.plan.Effect.CompleteDiaryTask
import ddm.ui.model.player.Cache
import ddm.ui.model.player.diary.{DiaryRegion, DiaryTask, DiaryTier}
import org.scalajs.dom.html.OList

object DiaryTaskList {
  def apply(
    completedTasksSignal: Signal[Set[Int]],
    cache: Cache,
    effectObserverSignal: Signal[Option[Observer[CompleteDiaryTask]]],
    contextMenuController: ContextMenu.Controller,
    regionFilterSignal: Signal[Option[DiaryRegion]],
    tierFilterSignal: Signal[Option[DiaryTier]],
    progressFilterSignal: Signal[Option[TaskDetailsTab.Progress]],
    searchFilterSignal: Signal[Option[List[DiaryTask]]]
  ): ReactiveHtmlElement[OList] = {
    val tasksSignal =
      Signal
        .combine(completedTasksSignal, regionFilterSignal, tierFilterSignal, progressFilterSignal, searchFilterSignal)
        .map((completedTasks, regionFilter, tierFilter, progressFilter, searchFilter) =>
          enumerateTasks(cache, completedTasks, regionFilter, tierFilter, progressFilter, searchFilter)
        )

    TaskList(
      tasksSignal,
      task => DiaryTaskElement(
        task,
        completedTasksSignal.map(_.contains(task.id)),
        effectObserverSignal,
        contextMenuController
      )
    )
  }

  private def enumerateTasks(
    cache: Cache,
    completedTasks: Set[Int],
    regionFilter: Option[DiaryRegion],
    tierFilter: Option[DiaryTier],
    progressFilter: Option[TaskDetailsTab.Progress],
    searchFilter: Option[List[DiaryTask]]
  ): List[DiaryTask] =
    searchFilter
      .getOrElse(cache.diaryTasks.values.toList.sortBy(task => (task.tier, task.region, task.id)))
      .filter(task =>
        regionFilter.forall(_ == task.region) &&
          tierFilter.forall(_ == task.tier) &&
          progressFilter.forall {
            case TaskDetailsTab.Progress.Incomplete => !completedTasks.contains(task.id)
            case TaskDetailsTab.Progress.Complete => completedTasks.contains(task.id)
          }
      )
}
