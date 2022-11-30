package ddm.ui.dom.common.form

import cats.data.NonEmptyList
import com.raquo.airstream.core.Signal
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, eventPropToProcessor, seqToModifier, textToNode}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html

object Select {
  final case class Opt[T](value: T, uniqueLabel: String)

  def apply[T](
    id: String,
    options: NonEmptyList[Opt[T]],
  ): (L.Select, L.Label, Signal[T]) = {
    val state = Var(options.head)
    (select(id, options.toList, state), label(id), state.signal.map(_.value))
  }

  private def select[T](
    id: String,
    options: List[Opt[T]],
    selection: Var[Opt[T]]
  ): L.Select =
    L.select(
      L.idAttr(id),
      L.name(id),
      L.controlled(
        L.value <-- selection.signal.map(_.uniqueLabel),
        L.onChange.mapToValue.map(label =>
          options
            .find(_.uniqueLabel == label)
            .getOrElse(throw new RuntimeException(s"Unexpected select [$id] option: [$label]"))
        ) --> selection
      ),
      options.map(option)
    )

  private def option[T](opt: Opt[T]): ReactiveHtmlElement[html.Option] =
    L.option(L.value(opt.uniqueLabel), opt.uniqueLabel)

  private def label(id: String): L.Label =
    L.label(L.forId(id))
}
