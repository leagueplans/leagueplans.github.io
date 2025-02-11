package com.leagueplans.ui.dom.player.diary

import com.leagueplans.ui.dom.player.task.TaskSummaryTab
import com.leagueplans.ui.model.player.Cache
import com.leagueplans.ui.model.player.diary.{DiaryRegion, DiaryTask, DiaryTier}
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.L

import scala.util.chaining.scalaUtilChainingOps

object DiarySummaryTab {
  def apply(
    completedTasksSignal: Signal[Set[Int]],
    cache: Cache,
    regionObserver: Observer[DiaryRegion],
    tierObserver: Observer[Option[DiaryTier]]
  ): L.Div = {
    val groupedTasks = groupTasks(cache)

    TaskSummaryTab(
      DiaryRegion.values.map { region =>
        val tiers = groupedTasks(region)
        DiaryOption(
          region,
          completedEasySignal = completedTasksSignal.map(isTierComplete(tiers(DiaryTier.Easy), _)),
          completedMediumSignal = completedTasksSignal.map(isTierComplete(tiers(DiaryTier.Medium), _)),
          completedHardSignal = completedTasksSignal.map(isTierComplete(tiers(DiaryTier.Hard), _)),
          completedEliteSignal = completedTasksSignal.map(isTierComplete(tiers(DiaryTier.Elite), _)),
          regionObserver = regionObserver.contramap[Unit](_ => region),
          tierObserver = tierObserver
        )
      }.toList
    )
  }

  private def groupTasks(cache: Cache): Map[DiaryRegion, Map[DiaryTier, List[DiaryTask]]] =
    cache
      .diaryTasks
      .values
      .groupBy(_.region)
      .map((region, tasks) =>
        tasks
          .groupBy(_.tier)
          .map((tier, tasks) => tier -> tasks.toList.sortBy(_.id))
          .pipe(region -> _)
      )

  private def isTierComplete(tierTasks: Iterable[DiaryTask], completedTasks: Set[Int]): Boolean =
    tierTasks.forall(task => completedTasks.contains(task.id))
}
