package com.leagueplans.ui.dom.planning.player.diary

import com.leagueplans.ui.dom.common.ContextMenu
import com.leagueplans.ui.dom.planning.player.task.{TaskDetailsTab, TaskList}
import com.leagueplans.ui.model.plan.Effect.CompleteDiaryTask
import com.leagueplans.ui.model.player.Cache
import com.leagueplans.ui.model.player.diary.{DiaryRegion, DiaryTask, DiaryTier}
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.nodes.ReactiveHtmlElement
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
