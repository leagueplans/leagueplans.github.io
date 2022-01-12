package ddm.ui.component.plan

import japgolly.scalajs.react.component.Scala.Unmounted
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, ScalaComponent}

object StepVisibilityComponent {
  final case class Props(visibility: Visibility, onClick: ^.onClick.Event => Callback)

  def apply(visibility: Visibility, onClick: ^.onClick.Event => Callback): Unmounted[Props, Unit, Unit] =
    ScalaComponent
      .builder[Props]
      .render_P(p =>
        <.button(
          ^.onClick ==> p.onClick,
          ^.className := "step-visibility",
          <.p(
            ^.className := "step-visibility-icon",
            s"[${if (p.visibility.isHidden) '+' else '-'}]"
          )
        )
      )
      .build
      .apply(Props(visibility, onClick))
}
