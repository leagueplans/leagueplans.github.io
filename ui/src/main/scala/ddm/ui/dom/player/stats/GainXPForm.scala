package ddm.ui.dom.player.stats

import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.laminar.api.{L, seqToModifier, textToTextNode}
import ddm.common.model.Skill
import ddm.ui.dom.common.form.{Form, NumberInput}
import ddm.ui.model.plan.Effect.GainExp
import ddm.ui.model.player.skill.Exp

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object GainXPForm {
  def apply(skill: Skill): (L.FormElement, EventStream[Option[GainExp]]) = {
    val (emptyForm, submitButton, formSubmissions) = Form()
    val (xpInputNodes, xpSignal) = xpInput()

    val form = emptyForm.amend(
      SkillIcon(skill).amend(L.cls(Styles.icon)),
      xpInputNodes,
      submitButton.amend(L.cls(Styles.input))
    )
    val submissions = effectSubmissions(skill, formSubmissions, xpSignal)

    (form, submissions)
  }

  @js.native @JSImport("/styles/player/stats/gainXPForm.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val label: String = js.native
    val input: String = js.native
    val icon: String = js.native
  }

  private def xpInput(): (List[L.Element], Signal[Double]) = {
    val (xpInput, xpLabel, quantitySignal) =
      NumberInput(id = "gain-xp-input", initial = 0.0)

    val nodes = List(
      xpLabel.amend(L.cls(Styles.label), "XP:"),
      xpInput.amend(
        L.cls(Styles.input),
        L.required(true),
        L.minAttr("0.1"),
        L.maxAttr("200000000"),
        L.stepAttr("0.1")
      )
    )

    (nodes, quantitySignal)
  }

  private def effectSubmissions(
    skill: Skill,
    formSubmissions: EventStream[Unit],
    xpSignal: Signal[Double]
  ): EventStream[Option[GainExp]] =
    formSubmissions.sample(
      xpSignal.map(xp =>
        Option.when(xp > 0)(GainExp(skill, Exp(xp)))
      )
    )
}
