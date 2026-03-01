package com.leagueplans.ui.dom.planning.player.stats.form

import com.leagueplans.common.model.Skill
import com.leagueplans.ui.dom.common.{Modal, Tooltip}
import com.leagueplans.ui.model.plan.{Effect, ExpMultiplier}
import com.leagueplans.ui.model.player.{Cache, Player}
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, StringSeqValueMapper, optionToModifier}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

//TODO Skill tabs aren't focused when pressing the tab keyboard key
object StatsDetailForm {
  def apply(
    activeSkill: Var[Skill],
    playerSignal: Signal[Player],
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    expMultipliers: List[ExpMultiplier],
    cache: Cache,
    tooltip: Tooltip
  ): L.Div = {
    val skillSignal = activeSkill.toObservable
    val gainExpForm = GainExpForm(skillSignal, playerSignal, expMultipliers, effectObserverSignal, cache, tooltip)
    val unlockSkillForm = UnlockSkillForm(skillSignal, effectObserverSignal)

    L.div(
      L.cls(Styles.form, Modal.Styles.form),
      StatsDetailTabs(activeSkill).amend(L.cls(Styles.tabs)),
      StatsDetailOverview(
        skillSignal,
        Signal.combine(playerSignal, skillSignal).map((player, skill) =>
          player.stats(skill)
        ),
        tooltip
      ).amend(L.cls(Styles.overviewPane)),
      L.child <-- toMutatorPane(
        skillSignal,
        playerSignal,
        effectObserverSignal,
        gainExpForm,
        unlockSkillForm
      ).map(_.amend(L.cls(Styles.mutatorPane))),
      Option.when(expMultipliers.nonEmpty)(
        MultiplierPane(skillSignal, playerSignal, expMultipliers, cache).amend(
          L.cls(Styles.multiplierPane)
        )
      )
    )
  }

  @js.native @JSImport("/styles/planning/player/stats/form/statsDetailForm.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val form: String = js.native
    val tabs: String = js.native
    val overviewPane: String = js.native
    val mutatorPane: String = js.native
    val multiplierPane: String = js.native
  }

  private def toMutatorPane(
    activeSkill: Signal[Skill],
    playerSignal: Signal[Player],
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    gainExpForm: L.HtmlElement,
    unlockSkillForm: L.HtmlElement
  ): Signal[L.HtmlElement] =
    Signal
      .combine(activeSkill, playerSignal, effectObserverSignal)
      .splitOne((skill, player, effectObserver) =>
        effectObserver.map(_ => player.leagueStatus.skillsUnlocked.contains(skill))
      )((state, _, _) =>
        state match {
          case Some(skillUnlocked) =>
            if (skillUnlocked) gainExpForm else unlockSkillForm
          case None =>
            L.div()
        }
      )
}
