package ddm.ui.component.common.form

import ddm.ui.component.common.DebounceComponent
import ddm.ui.component.common.form.FuseSearchComponent.{Backend, Props}
import ddm.ui.component.{Render, With, WithS}
import ddm.ui.wrappers.fusejs.Fuse
import japgolly.scalajs.react.component.Scala.Unmounted
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, CtorType, ScalaComponent}

import scala.concurrent.duration.DurationInt

final class FuseSearchComponent[T] {
  private val build: ScalaComponent[Props[T], Unit, Backend[T], CtorType.Props] =
    ScalaComponent
      .builder[Props[T]]
      .renderBackend[Backend[T]]
      .build

  def apply(
    fuse: Fuse[T],
    id: String,
    label: String,
    placeholder: String,
    maxResults: Int,
    defaultResults: List[T],
    render: Render[List[T]]
  ): Unmounted[Props[T], Unit, Backend[T]] =
    build(Props(fuse, id, label, placeholder, maxResults, defaultResults, render))
}

object FuseSearchComponent {
  final case class Props[T](
    fuse: Fuse[T],
    id: String,
    label: String,
    placeholder: String,
    maxResults: Int,
    defaultResults: List[T],
    render: Render[List[T]]
  )

  final class Backend[T](scope: BackendScope[Props[T], Unit]) {
    private val textInputComponent = TextInputComponent.build
    private val debounceComponent = new DebounceComponent[String, Option[List[T]]]

    def render(props: Props[T]): VdomNode =
      withSearchBox(props)((searchContent, searchBox) =>
        withDebounce(searchContent, search(props.fuse, props.maxResults)) {
          case Some(results) => props.render(results, searchBox)
          case None => props.render(props.defaultResults, searchBox)
        }
      )

    private def withSearchBox(props: Props[T]): With[String] =
      render => textInputComponent(TextInputComponent.Props(
        TextInputComponent.Type.Search,
        props.id,
        props.label,
        props.placeholder,
        render
      ))

    private def withDebounce(
      searchContent: String,
      expensiveComputation: String => Option[List[T]]
    ): WithS[Option[List[T]]] =
      render => debounceComponent(
        searchContent,
        delay = 250.milliseconds,
        expensiveComputation,
        render
      )

    private def search(fuse: Fuse[T], maxResults: Int): String => Option[List[T]] =
      query => Option.when(query.nonEmpty)(
        fuse.search(query, maxResults)
      )
  }
}
