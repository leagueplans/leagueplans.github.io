package ddm.ui.component.common.form

import ddm.ui.component.common.ThrottleComponent
import ddm.ui.component.{Render, With}
import ddm.ui.wrappers.fusejs.Fuse
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, CtorType, ScalaComponent}

import scala.concurrent.duration.DurationInt

object FuseSearchComponent {
  def build[T]: ScalaComponent[Props[T], Unit, Backend[T], CtorType.Props] =
    ScalaComponent
      .builder[Props[T]]
      .renderBackend[Backend[T]]
      .build

  final case class Props[T](
    fuse: Fuse[T],
    id: String,
    label: String,
    placeholder: String,
    limit: Int,
    defaultResults: List[T],
    render: Render[List[T]]
  )

  final class Backend[T](scope: BackendScope[Props[T], Unit]) {
    private val throttleComponent = ThrottleComponent.build[String]("")
    private val textInputComponent = TextInputComponent.build

    def render(props: Props[T]): VdomNode =
      withSearchBox(props)((searchBoxValue, searchBox) =>
        withThrottler(searchBoxValue) { fuseQuery =>
          val results =
            if (searchBoxValue.isEmpty)
              props.defaultResults
            else
              props.fuse.search(fuseQuery, props.limit)

          props.render(results, searchBox)
        }
      )

    private def withThrottler(searchBoxValue: String): (String => VdomNode) => VdomNode =
      render => throttleComponent(ThrottleComponent.Props(
        searchBoxValue,
        delay = 250.milliseconds,
        render
      ))

    private def withSearchBox(props: Props[T]): With[String] =
      render => textInputComponent(TextInputComponent.Props(
        TextInputComponent.Type.Search,
        id = props.id,
        label = props.label,
        placeholder = props.placeholder,
        render
      ))
  }
}
