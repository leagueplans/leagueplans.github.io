package com.leagueplans.ui.dom.planning.player.stats.form

import com.leagueplans.common.model.Skill
import com.leagueplans.ui.dom.common.{Modal, Tooltip}
import com.leagueplans.ui.dom.common.form.{Form, NumberInput}
import com.leagueplans.ui.model.plan.Effect.GainExp
import com.leagueplans.ui.model.plan.ExpMultiplier
import com.leagueplans.ui.model.player.{Cache, Player}
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
    cache: Cache,
    tooltip: Tooltip
  ): L.FormElement = {
    val (form, submitButton, formSubmissions) = Form()
    val actionsInput = createActionsInput()
    val expInput = createExpInput()
    val expSignal = Signal.combine(expInput.signal, actionsInput.signal).map(_ * _)

    form.amend(
      L.cls(Styles.form),
      L.p(
        L.cls(Styles.title),
        L.text <-- skillSignal.map(skill => s"Gain ${skill.toString.toLowerCase} xp")
      ),
      actionsInput.label,
      actionsInput.input,
      expInput.label,
      expInput.input,
      createProjection(
        skillSignal,
        playerSignal,
        calculatePostMultiplierGainedExp(skillSignal, playerSignal, expSignal, expMultipliers, cache),
        hasExpMultipliers = expMultipliers.nonEmpty,
        tooltip
      ),
      submitButton.amend(
        L.cls(Styles.submit, Modal.Styles.confirmationButton),
        L.value("Gain xp"),
        L.disabled <-- effectObserverSignal.map(_.isEmpty)
      ),
      bindEffectSubmissions(skillSignal, expSignal, formSubmissions, effectObserverSignal)
    )
  }

  @js.native @JSImport("/styles/planning/player/stats/form/gainExpForm.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val form: String = js.native
    val title: String = js.native

    val actionsLabel: String = js.native
    val actionsInput: String = js.native

    val expLabel: String = js.native
    val expInput: String = js.native

    val projection: String = js.native
    val explainer: String = js.native
    val progressBar: String = js.native

    val submit: String = js.native
  }

  private def createActionsInput(): (input: L.Input, label: L.Label, signal: Signal[Int]) = {
    val (input, label, signal) = NumberInput(id = "gain-exp-actions-input", initial = 1)

    label.amend(L.cls(Styles.actionsLabel), "Number of actions")
    input.amend(
      L.cls(Styles.actionsInput),
      L.required(true),
      L.stepAttr("1"),
      L.selectOnFocus
    )

    (input, label, signal)
  }

  private def createExpInput(): (input: L.Input, label: L.Label, signal: Signal[Exp]) = {
    val (input, label, signal) = NumberInput(id = "gain-exp-exp-input", initial = 0.0)

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
    hasExpMultipliers: Boolean,
    tooltip: Tooltip
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
        calculateEndExp(skillSignal, playerSignal, gainedExpSignal),
        tooltip
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
    expMultipliers: List[ExpMultiplier],
    cache: Cache
  ): Signal[Exp] =
    Signal
      .combine(skillSignal, playerSignal, gainedExpSignal)
      .map((skill, player, gainedExp) =>
        gainedExp * ExpMultiplier.calculateMultiplier(expMultipliers)(skill, player, cache)
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
