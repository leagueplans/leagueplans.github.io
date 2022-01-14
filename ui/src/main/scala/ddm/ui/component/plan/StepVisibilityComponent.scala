package ddm.ui.component.plan

import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, CtorType, ScalaComponent}

object StepVisibilityComponent {
  val build: Component[Props, Unit, Unit, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .render_P((render _).tupled)
      .build

  type Props = (Visibility, ^.onClick.Event => Callback)

  private def render(visibility: Visibility, onClick: ^.onClick.Event => Callback): VdomNode =
    <.button(
      ^.onClick ==> onClick,
      ^.className := "step-visibility-toggle",
      <.p(
        ^.className := "step-visibility-toggle-icon",
        s"[${if (visibility.isHidden) '+' else '-'}]"
      )
    )
}
