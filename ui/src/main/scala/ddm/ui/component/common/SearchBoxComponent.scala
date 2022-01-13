package ddm.ui.component.common

import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, CtorType, ReactEventFromInput, ScalaComponent}

object SearchBoxComponent {
  val build: Component[Props, Unit, Unit, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .render_P((render _).tupled)
      .build

  type Props = (String, String => Callback)

  private def render(currentValue: String, onChange: String => Callback): VdomNode =
    <.input.text(
      ^.placeholder := "Search...",
      ^.value := currentValue,
      ^.onChange ==> { event: ReactEventFromInput =>
        onChange(event.target.value)
      }
    )
}
