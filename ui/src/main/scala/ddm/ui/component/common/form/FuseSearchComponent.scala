package ddm.ui.component.common.form

import ddm.ui.component.common.ThrottleComponent
import ddm.ui.component.{Render, With}
import ddm.ui.wrappers.fusejs.Fuse
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{CtorType, ScalaComponent}

import scala.concurrent.duration.DurationInt

object FuseSearchComponent {
  def build[T]: Component[Props[T], Unit, Unit, CtorType.Props] =
    ScalaComponent
      .builder[Props[T]]
      .render_P(render)
      .build

  final case class Props[T](fuse: Fuse[T], limit: Int, render: Render[List[T]])

  private val withSearchBox: With[String] = SearchBoxComponent.build(_)
  private val throttler = ThrottleComponent.build[String]("")

  private def render[T](props: Props[T]): VdomNode =
    withSearchBox((searchBoxValue, searchBox) =>
      withThrottler(searchBoxValue) { fuseQuery =>
        val results = props.fuse.search(fuseQuery, props.limit)
        props.render(results, searchBox)
      }
    )

  private def withThrottler(searchBoxValue: String): (String => VdomNode) => VdomNode =
    render => throttler(ThrottleComponent.Props(
      searchBoxValue,
      delay = 250.milliseconds,
      render
    ))
}
