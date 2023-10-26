package ddm.ui.dom.player.league

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.{Val, Var}
import com.raquo.laminar.api.L
import ddm.common.model.{LeagueTaskArea, LeagueTaskTier, ShatteredRelicsTaskProperties}
import ddm.ui.dom.common.ContextMenu
import ddm.ui.dom.player.task.{TaskDetailsTab, TaskFilters}
import ddm.ui.facades.fusejs.FuseOptions
import ddm.ui.model.plan.Effect.CompleteLeagueTask
import ddm.ui.model.player.Cache
import ddm.ui.model.player.mode.Mode
import ddm.ui.wrappers.fusejs.Fuse

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
        LeagueTaskTier.all.map(tier => (tier, tier.toString)),
        tierVar
      ),
      TaskFilters.Filter(
        id = "area",
        label = "Area (TBL):",
        LeagueTaskArea.all.map(area => (area, area.name)),
        areaVar
      ),
      TaskFilters.Filter(
        id = "category",
        label = "Category (SRL):",
        ShatteredRelicsTaskProperties.Category.all.map(category => (category, category.name)),
        leagues3CategoryVar
      )
    ))

    val fuse = new Fuse(
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
