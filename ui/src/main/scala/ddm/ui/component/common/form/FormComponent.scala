package ddm.ui.component.common.form

import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, CtorType, ReactFormEvent, ScalaComponent}

object FormComponent {
  val build: Component[Props, Unit, Unit, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .render_P(render)
      .build

  final case class Props(
    onSubmit: Callback,
    formContents: TagMod
  )

  private def render(props: Props): VdomNode =
    <.form(
      props.formContents,
      <.input.submit(),
      ^.onSubmit ==> { e: ReactFormEvent =>
        e.preventDefault()
        props.onSubmit
      }
    )
}
