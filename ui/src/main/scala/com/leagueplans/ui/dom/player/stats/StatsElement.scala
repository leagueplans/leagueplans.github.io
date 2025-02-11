package com.leagueplans.ui.dom.player.stats

import com.leagueplans.common.model.Skill
import com.leagueplans.common.model.Skill.*
import com.leagueplans.ui.dom.common.{ContextMenu, Modal}
import com.leagueplans.ui.model.plan.Effect
import com.leagueplans.ui.model.player.Player
import com.leagueplans.ui.model.player.skill.{Stat, Stats}
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.L
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html.OList

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object StatsElement {
  def from(
    playerSignal: Signal[Player],
    effectObserver: Signal[Option[Observer[Effect]]],
    contextMenuController: ContextMenu.Controller,
    modalController: Modal.Controller
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
      contextMenuController,
      modalController
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
    contextMenuController: ContextMenu.Controller,
    modalController: Modal.Controller
  ): ReactiveHtmlElement[OList] = {
    val statPanes = statsSignal.split(_.skill)((_, _, signal) =>
      L.li(StatPane(signal, effectObserver, contextMenuController, modalController))
    )

    L.ol(
      L.cls(Styles.stats),
      L.children <-- statPanes,
      L.li(TotalLevelPane(statsSignal.map(stats =>
        Stats(stats.map(stat => stat.skill -> stat.exp).toMap))
      ))
    )
  }

  @js.native @JSImport("/styles/player/stats/statsElement.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val stats: String = js.native
  }
}
