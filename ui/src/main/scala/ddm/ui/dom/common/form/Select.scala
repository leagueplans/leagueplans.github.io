package ddm.ui.dom.common.form

import cats.data.NonEmptyList
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, eventPropToProcessor, seqToModifier, textToTextNode}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html

object Select {
  final case class Opt[+T](value: T, uniqueLabel: String)

  def apply[T](
    id: String,
    options: NonEmptyList[Opt[T]],
  ): (L.Select, L.Label, Signal[T]) = {
    val state = Var(options.head)
    (select(id, options.toList, state.writer, state.signal), label(id), state.signal.map(_.value))
  }

  def apply[T](
    id: String,
    options: List[Opt[T]],
    selection: Var[T]
  ): (L.Select, L.Label) = {
    val optMap = options.map(opt => opt.value -> opt).toMap
    val observer = selection.writer.contramap[Opt[T]](_.value)
    val signal = selection.signal.map(optMap)
    (select(id, options, observer, signal), label(id))
  }

  private def select[T](
    id: String,
    options: List[Opt[T]],
    selectionObserver: Observer[Opt[T]],
    selectionSignal: Signal[Opt[T]]
  ): L.Select =
    L.select(
      L.idAttr(id),
      L.nameAttr(id),
      L.controlled(
        L.value <-- selectionSignal.map(_.uniqueLabel),
        L.onChange.mapToValue.map(label =>
          options
            .find(_.uniqueLabel == label)
            .getOrElse(throw new RuntimeException(s"Unexpected select [$id] option: [$label]"))
        ) --> selectionObserver
      ),
      options.map(option)
    )

  private def option[T](opt: Opt[T]): ReactiveHtmlElement[html.Option] =
    L.option(L.value(opt.uniqueLabel), opt.uniqueLabel)

  private def label(id: String): L.Label =
    L.label(L.forId(id))
}
