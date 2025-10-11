package com.leagueplans.ui.dom.planning.player.stats.form

import com.leagueplans.common.model.Skill
import com.leagueplans.ui.dom.common.Modal
import com.leagueplans.ui.dom.common.form.{Form, NumberInput}
import com.leagueplans.ui.model.plan.Effect.GainExp
import com.leagueplans.ui.model.plan.ExpMultiplier
import com.leagueplans.ui.model.player.Player
import com.leagueplans.ui.model.player.skill.Exp
import com.leagueplans.ui.utils.laminar.LaminarOps.selectOnFocus
import com.raquo.airstream.core.{EventStream, Observer, Signal}
import com.raquo.laminar.api.{L, StringSeqValueMapper, enrichSource, optionToModifier, textToTextNode}
import com.raquo.laminar.modifiers.Binder

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object GainExpForm {
  def apply(
    skillSignal: Signal[Skill],
    playerSignal: Signal[Player],
    expMultipliers: List[ExpMultiplier],
    effectObserverSignal: Signal[Option[Observer[GainExp]]],
  ): L.FormElement = {
    val (form, submitButton, formSubmissions) = Form()
    val expInput = createExpInput()

    form.amend(
      L.cls(Styles.form),
      L.p(
        L.cls(Styles.title),
        L.text <-- skillSignal.map(skill => s"Gain ${skill.toString.toLowerCase} xp")
      ),
      expInput.label,
      expInput.input,
      createProjection(
        skillSignal,
        playerSignal,
        calculatePostMultiplierGainedExp(skillSignal, playerSignal, expInput.signal, expMultipliers),
        hasExpMultipliers = expMultipliers.nonEmpty
      ),
      submitButton.amend(
        L.cls(Styles.submit, Modal.Styles.confirmationButton),
        L.value("Gain xp"),
        L.disabled <-- effectObserverSignal.map(_.isEmpty)
      ),
      bindEffectSubmissions(skillSignal, expInput.signal, formSubmissions, effectObserverSignal)
    )
  }

  @js.native @JSImport("/styles/planning/player/stats/form/gainExpForm.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val form: String = js.native
    val title: String = js.native

    val expLabel: String = js.native
    val expInput: String = js.native

    val projection: String = js.native
    val explainer: String = js.native
    val progressBar: String = js.native

    val submit: String = js.native
  }

  private def createExpInput(): (input: L.Input, label: L.Label, signal: Signal[Exp]) = {
    val (input, label, signal) = NumberInput(id = "gain-exp-input", initial = 0.0)

    label.amend(L.cls(Styles.expLabel), "Base xp")
    input.amend(
      L.cls(Styles.expInput),
      L.required(true),
      L.minAttr("0.1"),
      L.maxAttr("200000000"),
      L.stepAttr("0.1"),
      L.selectOnFocus
    )

    (input, label, signal.map(Exp.apply))
  }

  private def createProjection(
    skillSignal: Signal[Skill],
    playerSignal: Signal[Player],
    gainedExpSignal: Signal[Exp],
    hasExpMultipliers: Boolean
  ): L.Div =
    L.div(
      L.cls(Styles.projection),
      Option.when(hasExpMultipliers)(
        L.p(
          L.cls(Styles.explainer),
          L.text <-- gainedExpSignal.map(exp =>
            s"After multipliers, this will grant $exp xp"
          )
        )
      ),
      ProgressBar(
        skillSignal,
        calculateEndExp(skillSignal, playerSignal, gainedExpSignal)
      ).amend(L.cls(Styles.progressBar))
    )

  private def calculateEndExp(
    skillSignal: Signal[Skill],
    playerSignal: Signal[Player],
    gainedExpSignal: Signal[Exp]
  ): Signal[Exp] =
    Signal
      .combine(skillSignal, playerSignal, gainedExpSignal)
      .map((skill, player, gainedExp) => player.stats(skill) + gainedExp)

  private def calculatePostMultiplierGainedExp(
    skillSignal: Signal[Skill],
    playerSignal: Signal[Player],
    gainedExpSignal: Signal[Exp],
    expMultipliers: List[ExpMultiplier]
  ): Signal[Exp] =
    Signal
      .combine(skillSignal, playerSignal, gainedExpSignal)
      .map((skill, player, gainedExp) =>
        gainedExp * ExpMultiplier.calculateMultiplier(expMultipliers)(skill, player)
      )

  private def bindEffectSubmissions(
    skillSignal: Signal[Skill],
    expSignal: Signal[Exp],
    formSubmissions: EventStream[Unit],
    effectObserverSignal: Signal[Option[Observer[GainExp]]]
  ): Binder.Base =
    formSubmissions
      .sample(effectObserverSignal)
      .collectSome
      .withCurrentValueOf(skillSignal, expSignal)
      .filter((_, _, exp) => exp.raw > 0) --> ((observer, skill, exp) =>
        observer.onNext(GainExp(skill, exp))
      )
}
