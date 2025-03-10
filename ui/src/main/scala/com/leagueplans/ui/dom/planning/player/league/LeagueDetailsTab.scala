package com.leagueplans.ui.dom.planning.player.league

import com.leagueplans.common.model.{LeagueTaskArea, LeagueTaskTier, ShatteredRelicsTaskProperties}
import com.leagueplans.ui.dom.common.ContextMenu
import com.leagueplans.ui.dom.planning.player.task.{TaskDetailsTab, TaskFilters}
import com.leagueplans.ui.facades.fusejs.FuseOptions
import com.leagueplans.ui.model.plan.Effect.CompleteLeagueTask
import com.leagueplans.ui.model.player.Cache
import com.leagueplans.ui.model.player.mode.Mode
import com.leagueplans.ui.wrappers.fusejs.Fuse
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.{Val, Var}
import com.raquo.laminar.api.L

import scala.scalajs.js

object LeagueDetailsTab {
  def apply(
    completedTasksSignal: Signal[Set[Int]],
    cache: Cache,
    effectObserverSignal: Signal[Option[Observer[CompleteLeagueTask]]],
    contextMenuController: ContextMenu.Controller,
    leagueVar: Var[Option[Mode.League]]
  ): L.Div = {
    val tierVar = Var(Option.empty[LeagueTaskTier])
    val areaVar = Var(Option.empty[LeagueTaskArea])
    val leagues3CategoryVar = Var(Option.empty[ShatteredRelicsTaskProperties.Category])

    val filters = Val(List(
      TaskFilters.Filter(
        id = "league",
        label = "League:",
        Mode.League.all.map(league => (league, league.name)),
        leagueVar
      ),
      TaskFilters.Filter(
        id = "tier",
        label = "Tier:",
        LeagueTaskTier.values.map(tier => (tier, tier.toString)).toList,
        tierVar
      ),
      TaskFilters.Filter(
        id = "area",
        label = "Area (TBL):",
        LeagueTaskArea.values.map(area => (area, area.name)).toList,
        areaVar
      ),
      TaskFilters.Filter(
        id = "category",
        label = "Category (SRL):",
        ShatteredRelicsTaskProperties.Category.values.map(category => (category, category.name)).toList,
        leagues3CategoryVar
      )
    ))

    val fuse = Fuse(
      cache.leagueTasks.values.toList.sortBy(_.id),
      new FuseOptions {
        keys = js.defined(js.Array("name", "description"))
      }
    )

    TaskDetailsTab(
      taskType = "league",
      fuse,
      filters,
      LeagueTaskList(
        completedTasksSignal,
        cache,
        effectObserverSignal,
        contextMenuController,
        leagueVar.signal,
        tierVar.signal,
        _,
        areaVar.signal,
        leagues3CategoryVar.signal,
        _
      )
    )
  }
}
