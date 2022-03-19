package ddm.ui.component.common

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, CtorType, ScalaComponent}

object DualColumnListComponent {
  val build: ScalaComponent[Props, Unit, Backend, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .renderBackend[Backend]
      .build

  type Props = List[(TagMod, TagMod)]

  final class Backend(scope: BackendScope[Props, Unit]) {
    def render(props: Props): VdomNode =
      <.table(
        ^.className := "dual-column-list",
        <.tbody(
          props.toTagMod { case (key, value) =>
            <.tr(
              <.td(
                ^.className := "dual-column-list left",
                key
              ),
              <.td(
                ^.className := "dual-column-list right",
                value
              )
            )
          }
        )
      )
  }
}
