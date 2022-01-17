package ddm.ui.component.plan.editing.effect

import cats.data.NonEmptyList
import ddm.ui.component.With
import ddm.ui.component.common.RadioButtonComponent
import ddm.ui.model.plan.Effect
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, CtorType, ScalaComponent}

object AddEffectComponent {
  val build: Component[Props, Unit, Unit, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .render_P(render)
      .build

  type Props = Effect => Callback

  private type EffectComponent = Component[Effect => Callback, Unit, Unit, CtorType.Props]
  private val effectSelectComponent = RadioButtonComponent.build[EffectComponent]
  private val effects = NonEmptyList.of("Gain XP" -> GainExpComponent.build)

  private val withEffectSelect: With[EffectComponent] =
    render => effectSelectComponent(RadioButtonComponent.Props(
      name = "effect-select",
      effects,
      render
    ))

  private def render(props: Props): VdomNode =
    withEffectSelect((component, effectSelect) =>
      <.div(
        ^.className := "add-effect",
        effectSelect,
        component(props)
      )
    )
}
