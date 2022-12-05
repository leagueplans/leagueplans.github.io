package ddm.ui.dom.player.stats

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.L
import com.raquo.laminar.nodes.ReactiveHtmlElement
import ddm.ui.dom.common.ContextMenu
import ddm.ui.model.plan.Effect
import ddm.ui.model.player.Player
import ddm.ui.model.player.skill.Skill._
import ddm.ui.model.player.skill.{Skill, Stat, Stats}
import org.scalajs.dom.html.OList

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object StatsElement {
  def from(
    playerSignal: Signal[Player],
    effectObserver: Signal[Option[Observer[Effect]]],
    contextMenuController: ContextMenu.Controller
  ): ReactiveHtmlElement[OList] =
    StatsElement(
      playerSignal.map(player =>
        orderedSkills.map(skill =>
          Stat(
            skill,
            player.stats(skill),
            player.leagueStatus.skillsUnlocked.contains(skill)
          )
        )
      ),
      effectObserver,
      contextMenuController
    )

  private val orderedSkills: List[Skill] =
    List(
      Attack,       Hitpoints, Mining,
      Strength,     Agility,   Smithing,
      Defence,      Herblore,  Fishing,
      Ranged,       Thieving,  Cooking,
      Prayer,       Crafting,  Firemaking,
      Magic,        Fletching, Woodcutting,
      Runecraft,    Slayer,    Farming,
      Construction, Hunter
    )

  def apply(
    statsSignal: Signal[List[Stat]],
    effectObserver: Signal[Option[Observer[Effect]]],
    contextMenuController: ContextMenu.Controller
  ): ReactiveHtmlElement[OList] = {
    val statPanes = statsSignal.split(_.skill) { (_, _, signal) =>
      val (modal, pane) = StatPane(signal, effectObserver, contextMenuController)
      L.li(modal, pane)
    }

    L.ol(
      L.cls(Styles.stats),
      L.children <-- statPanes,
      TotalLevelPane(statsSignal.map(stats =>
        Stats(stats.map(stat => stat.skill -> stat.exp).toMap))
      )
    )
  }

  @js.native @JSImport("/styles/player/stats/statsElement.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val stats: String = js.native
  }
}
