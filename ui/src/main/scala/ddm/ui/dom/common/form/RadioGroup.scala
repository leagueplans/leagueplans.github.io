package ddm.ui.dom.common.form

import cats.data.NonEmptyList
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, enrichSource, eventPropToProcessor, seqToModifier}

object RadioGroup {
  final case class Opt[T](value: T, id: String)

  /** The first option is chosen as the default */
  def apply[T](
    groupName: String,
    options: NonEmptyList[Opt[T]],
    render: (T, L.Input, L.Label) => L.Children
  ): (L.Children, Signal[T]) = {
    val default = options.head
    val selection = Var(default)
    val rendering =
      option(default, groupName, initiallyChecked = true, selection.writer, render) +:
        options.tail.map(
          option(_, groupName, initiallyChecked = false, selection.writer, render)
        )

    (rendering.flatten, selection.signal.map(_.value))
  }

  /** No option is selected by default. Selections are maintained so long as
    * they remain in the list of options. */
  def apply[T](
    groupName: String,
    options: Signal[List[Opt[T]]],
    render: (T, L.Input, L.Label) => L.Children
  ): (L.Modifier[L.Element], Signal[Option[T]]) = {
    val selection = Var[Option[Opt[T]]](None)
    val rendering =
      options.split(_.id) { case (_, opt, _) =>
        option(
          opt,
          groupName,
          initiallyChecked = false,
          selection.writer.contramapSome[Opt[T]],
          render
        )
      }.map(_.flatten)

    val bindings =
      List(
        options --> clearSelectionIfOptionRemoved(selection),
        L.children <-- rendering
      )

    (bindings, selection.signal.map(_.map(_.value)))
  }

  private def option[T](
    opt: Opt[T],
    groupName: String,
    initiallyChecked: Boolean,
    selection: Observer[Opt[T]],
    render: (T, L.Input, L.Label) => L.Children
  ): L.Children = {
    val id = s"$groupName-${opt.id}"
    val checked = selection.contracollect[Boolean] { case true => opt }
    render(
      opt.value,
      radio(id, groupName, initiallyChecked, checked),
      label(id)
    )
  }

  private def radio(
    id: String,
    groupName: String,
    initiallyChecked: Boolean,
    checked: Observer[Boolean]
  ): L.Input =
    L.input(
      L.`type`("radio"),
      L.idAttr(id),
      L.name(groupName),
      L.defaultChecked(initiallyChecked),
      L.onClick.mapToChecked.setAsChecked --> checked,
    )

  private def label(id: String): L.Label =
    L.label(L.forId(id))

  private def clearSelectionIfOptionRemoved[T](
    selection: Var[Option[Opt[T]]],
  ): Observer[List[Opt[T]]] =
    selection.updater[List[Opt[T]]] {
      case (Some(opt), updatedOptions) if updatedOptions.contains(opt) =>
        Some(opt)
      case _ =>
        None
    }
}
