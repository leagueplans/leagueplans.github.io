package com.leagueplans.ui.dom.planning.player.stats.form

import com.leagueplans.common.model.Skill
import com.leagueplans.ui.dom.common.{Button, Modal}
import com.leagueplans.ui.model.plan.Effect.UnlockSkill
import com.leagueplans.ui.utils.laminar.EventProcessorOps.handledWith
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, StringSeqValueMapper, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object UnlockSkillForm {
  def apply(
    skillSignal: Signal[Skill],
    effectObserverSignal: Signal[Option[Observer[UnlockSkill]]],
  ): L.Div =
    L.div(
      L.cls(Styles.form),
      L.p(L.cls(Styles.title), "This skill is currently locked"),
      createUnlockButton(skillSignal, effectObserverSignal).amend(
        L.cls(Styles.button, Modal.Styles.confirmationButton)
      )
    )

  @js.native @JSImport("/styles/planning/player/stats/form/unlockSkillForm.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val form: String = js.native
    val title: String = js.native
    val button: String = js.native
  }
    
  private def createUnlockButton(
    skillSignal: Signal[Skill],
    effectObserverSignal: Signal[Option[Observer[UnlockSkill]]]
  ): L.Button =
    Button(
      _.handledWith(
        _.sample(effectObserverSignal)
          .collectSome
          .withCurrentValueOf(skillSignal)
      ) --> createClickObserver
    ).amend(
      "Unlock",
      L.disabled <-- effectObserverSignal.map(_.isEmpty)
    )

  private def createClickObserver: Observer[(Observer[UnlockSkill], Skill)] =
    Observer((observer, skill) => observer.onNext(UnlockSkill(skill)))
}
