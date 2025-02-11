package com.leagueplans.ui.dom.player.diary

import com.leagueplans.ui.dom.common.ContextMenu
import com.leagueplans.ui.dom.player.task.{TaskDetailsTab, TaskFilters}
import com.leagueplans.ui.facades.fusejs.FuseOptions
import com.leagueplans.ui.model.plan.Effect.CompleteDiaryTask
import com.leagueplans.ui.model.player.Cache
import com.leagueplans.ui.model.player.diary.{DiaryRegion, DiaryTier}
import com.leagueplans.ui.wrappers.fusejs.Fuse
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.{Val, Var}
import com.raquo.laminar.api.L

import scala.scalajs.js

object DiaryDetailsTab {
  def apply(
    completedTasksSignal: Signal[Set[Int]],
    cache: Cache,
    effectObserverSignal: Signal[Option[Observer[CompleteDiaryTask]]],
    contextMenuController: ContextMenu.Controller,
    regionVar: Var[Option[DiaryRegion]],
    tierVar: Var[Option[DiaryTier]]
  ): L.Div = {
    val filters = Val(List(
      TaskFilters.Filter(
        id = "region",
        label = "Region:",
        DiaryRegion.values.map(region => (region, region.name)).toList,
        regionVar
      ),
      TaskFilters.Filter(
        id = "tier",
        label = "Tier:",
        DiaryTier.values.map(tier => (tier, tier.toString)).toList,
        tierVar
      )
    ))

    val fuse = Fuse(
      cache.diaryTasks.values.toList.sortBy(_.id),
      new FuseOptions {
        keys = js.defined(js.Array("description"))
      }
    )

    TaskDetailsTab(
      taskType = "diary",
      fuse,
      filters,
      DiaryTaskList(
        completedTasksSignal,
        cache,
        effectObserverSignal,
        contextMenuController,
        regionVar.signal,
        tierVar.signal,
        _,
        _
      )
    )
  }
}
