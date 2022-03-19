package ddm.ui.component.common.form

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, CtorType, ReactFormEvent, ScalaComponent}

object FormComponent {
  val build: ScalaComponent[Props, Unit, Backend, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .renderBackend[Backend]
      .build

  final case class Props(onSubmit: Callback, formContents: TagMod)

  final class Backend(scope: BackendScope[Props, Unit]) {
    def render(props: Props): VdomNode =
      <.form(
        props.formContents,
        <.input.submit(),
        ^.onSubmit ==> { e: ReactFormEvent =>
          e.preventDefault()
          props.onSubmit
        }
      )
  }
}
