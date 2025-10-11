package com.leagueplans.ui.dom.planning.player.stats.form

import com.leagueplans.common.model.Skill
import com.leagueplans.ui.dom.common.Modal
import com.leagueplans.ui.model.plan.{Effect, ExpMultiplier}
import com.leagueplans.ui.model.player.Player
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
    expMultipliers: List[ExpMultiplier]
  ): L.Div = {
    val skillSignal = activeSkill.toObservable
    val gainExpForm = GainExpForm(skillSignal, playerSignal, expMultipliers, effectObserverSignal)
    val unlockSkillForm = UnlockSkillForm(skillSignal, effectObserverSignal)

    L.div(
      L.cls(Styles.form, Modal.Styles.form),
      StatsDetailTabs(activeSkill).amend(L.cls(Styles.tabs)),
      StatsDetailOverview(
        skillSignal,
        Signal.combine(playerSignal, skillSignal).map((player, skill) =>
          player.stats(skill)
        )
      ).amend(L.cls(Styles.overviewPane)),
      L.child <-- toMutatorPane(
        skillSignal,
        playerSignal,
        gainExpForm,
        unlockSkillForm
      ).map(_.amend(L.cls(Styles.mutatorPane))),
      Option.when(expMultipliers.nonEmpty)(
        MultiplierPane(skillSignal, playerSignal, expMultipliers).amend(
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
    gainExpForm: L.HtmlElement,
    unlockSkillForm: L.HtmlElement
  ): Signal[L.HtmlElement] =
    Signal
      .combine(activeSkill, playerSignal)
      .splitOne(
        (skill, player) => player.leagueStatus.skillsUnlocked.contains(skill)
      )((skillUnlocked, _, _) =>
        if (skillUnlocked) gainExpForm else unlockSkillForm
      )
}
