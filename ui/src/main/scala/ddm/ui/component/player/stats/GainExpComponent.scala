package ddm.ui.component.player.stats

import ddm.ui.component.With
import ddm.ui.component.common.form.{FormComponent, NumberInputComponent}
import ddm.ui.model.plan.Effect.GainExp
import ddm.ui.model.player.skill.{Exp, Skill}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, CtorType, ScalaComponent}

object GainExpComponent {
  val build: ScalaComponent[Props, Unit, Backend, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .renderBackend[Backend]
      .build

  final case class Props(skill: Skill, onSubmit: Option[GainExp] => Callback)

  final class Backend(scope: BackendScope[Props, Unit]) {
    private val iconComponent = SkillIconComponent.build
    private val xpInputComponent = NumberInputComponent.build[Double](initial = 0)
    private val formComponent = FormComponent.build

    def render(props: Props): VdomNode =
      withXpInput((xp, xpInput) =>
        formComponent(FormComponent.Props(
          props.onSubmit(Option.when(xp > 0)(GainExp(props.skill, Exp(xp)))),
          formContents = <.span(
            iconComponent(SkillIconComponent.Props(props.skill)),
            xpInput
          )
        ))
      )

    private val withXpInput: With[Double] =
      render => xpInputComponent(NumberInputComponent.Props(
        id = "xp-entry",
        label = "Gain XP:",
        min = 0,
        max = 200000000,
        step = 0.1,
        render
      ))
  }
}
