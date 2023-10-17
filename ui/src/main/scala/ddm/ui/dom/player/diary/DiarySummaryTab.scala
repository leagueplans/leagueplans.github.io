package ddm.ui.dom.player.diary

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, seqToModifier}
import ddm.ui.model.player.Cache
import ddm.ui.model.player.diary.{DiaryRegion, DiaryTask, DiaryTier}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import scala.util.chaining.scalaUtilChainingOps

object DiarySummaryTab {
  def apply(
    completedTasksSignal: Signal[Set[Int]],
    cache: Cache,
    regionObserver: Observer[DiaryRegion],
    tierObserver: Observer[Option[DiaryTier]]
  ): L.Div = {
    val groupedTasks = groupTasks(cache)

    L.div(
      L.cls(Styles.diaryOptions),
      DiaryRegion.all.map { region =>
        val tiers = groupedTasks(region)
        DiaryOption(
          region,
          completedEasySignal = completedTasksSignal.map(isTierComplete(tiers(DiaryTier.Easy), _)),
          completedMediumSignal = completedTasksSignal.map(isTierComplete(tiers(DiaryTier.Medium), _)),
          completedHardSignal = completedTasksSignal.map(isTierComplete(tiers(DiaryTier.Hard), _)),
          completedEliteSignal = completedTasksSignal.map(isTierComplete(tiers(DiaryTier.Elite), _)),
          regionObserver = regionObserver.contramap[Unit](_ => region),
          tierObserver = tierObserver
        ).amend(L.cls(Styles.option))
      }
    )
  }

  @js.native @JSImport("/styles/player/diary/diarySummaryTab.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val diaryOptions: String = js.native
    val option: String = js.native
  }

  private def groupTasks(cache: Cache): Map[DiaryRegion, Map[DiaryTier, List[DiaryTask]]] =
    cache
      .diaryTasks
      .values
      .groupBy(_.region)
      .map { case (region, tasks) =>
        tasks
          .groupBy(_.tier)
          .map { case (tier, tasks) => tier -> tasks.toList.sortBy(_.id) }
          .pipe(region -> _)
      }

  private def isTierComplete(tierTasks: Iterable[DiaryTask], completedTasks: Set[Int]): Boolean =
    tierTasks.forall(task => completedTasks.contains(task.id))
}
