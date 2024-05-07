package ddm.ui.dom.player.league

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import ddm.common.model.{LeagueTask, LeagueTaskArea, LeagueTaskTier, ShatteredRelicsTaskProperties}
import ddm.ui.dom.common.ContextMenu
import ddm.ui.dom.player.task.{TaskDetailsTab, TaskList}
import ddm.ui.model.plan.Effect.CompleteLeagueTask
import ddm.ui.model.player.Cache
import ddm.ui.model.player.mode.*
import ddm.ui.utils.HasID
import org.scalajs.dom.html.OList

object LeagueTaskList {
  private given HasID.Aux[LeagueTask, Int] = HasID(_.id)
  
  def apply(
    completedTasksSignal: Signal[Set[Int]],
    cache: Cache,
    effectObserverSignal: Signal[Option[Observer[CompleteLeagueTask]]],
    contextMenuController: ContextMenu.Controller,
    leagueFilterSignal: Signal[Option[Mode.League]],
    tierFilterSignal: Signal[Option[LeagueTaskTier]],
    progressFilterSignal: Signal[Option[TaskDetailsTab.Progress]],
    areaFilterSignal: Signal[Option[LeagueTaskArea]],
    leagues3CategoryFilterSignal: Signal[Option[ShatteredRelicsTaskProperties.Category]],
    searchFilterSignal: Signal[Option[List[LeagueTask]]]
  ): ReactiveHtmlElement[OList] = {
    val tasksSignal =
      Signal
        .combine(
          completedTasksSignal,
          leagueFilterSignal,
          tierFilterSignal,
          progressFilterSignal,
          areaFilterSignal,
          leagues3CategoryFilterSignal,
          searchFilterSignal
        )
        .map((completedTasks, leagueFilter, tierFilter, progressFilter, areaFilter, leagues3CategoryFilter, searchFilter) =>
          enumerateTasks(
            cache,
            completedTasks,
            leagueFilter,
            tierFilter,
            progressFilter,
            areaFilter,
            leagues3CategoryFilter,
            searchFilter
          )
        )

    TaskList(
      tasksSignal,
      task => LeagueTaskElement(
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
    leagueFilter: Option[Mode.League],
    tierFilter: Option[LeagueTaskTier],
    progressFilter: Option[TaskDetailsTab.Progress],
    areaFilter: Option[LeagueTaskArea],
    leagues3CategoryFilter: Option[ShatteredRelicsTaskProperties.Category],
    searchFilter: Option[List[LeagueTask]]
  ): List[LeagueTask] =
    searchFilter
      .getOrElse(cache.leagueTasks.values.toList.sortBy(_.id))
      .filter { task =>
        combine(leagueFilter, tierFilter, areaFilter, leagues3CategoryFilter)(task) &&
          progressFilter.forall {
            case TaskDetailsTab.Progress.Incomplete => !completedTasks.contains(task.id)
            case TaskDetailsTab.Progress.Complete => completedTasks.contains(task.id)
          }
      }

  private def combine(
    leagueFilter: Option[Mode.League],
    tierFilter: Option[LeagueTaskTier],
    areaFilter: Option[LeagueTaskArea],
    leagues3CategoryFilter: Option[ShatteredRelicsTaskProperties.Category]
  ): LeagueTask => Boolean =
    task => leagueFilter match {
      case Some(LeaguesI) =>
        task.leagues1Props.exists(tier => tierFilter.forall(_ == tier))

      case Some(LeaguesII) =>
        task.leagues2Props.exists(props =>
          tierFilter.forall(_ == props.tier) && areaFilter.forall(_ == props.area)
        )

      case Some(LeaguesIII) =>
        task.leagues3Props.exists(props =>
          tierFilter.forall(_ == props.tier) && leagues3CategoryFilter.forall(_ == props.category)
        )

      case Some(LeaguesIV) =>
        task.leagues4Props.exists(props =>
          tierFilter.forall(_ == props.tier) && areaFilter.forall(_ == props.area)
        )

      case Some(_) =>
        false

      case None =>
        tierFilter.forall(tier =>
          task.leagues1Props.contains(tier) ||
            task.leagues2Props.exists(_.tier == tier) ||
            task.leagues3Props.exists(_.tier == tier) ||
            task.leagues4Props.exists(_.tier == tier)
        ) && areaFilter.forall(area =>
          task.leagues2Props.exists(_.area == area) ||
          task.leagues4Props.exists(_.area == area)
        ) && leagues3CategoryFilter.forall(category =>
          task.leagues3Props.exists(_.category == category)
        )
    }
}
