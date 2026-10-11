package com.leagueplans.uicommon.dom.form

import cats.data.NonEmptyList
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, eventPropToProcessor, seqToModifier, textToTextNode}
import com.raquo.laminar.codecs.StringAsIsCodec
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html

object Select {
  final case class Opt[+T](value: T, uniqueLabel: String)

  def apply[T](
    id: String,
    options: NonEmptyList[Opt[T]],
  ): (L.Select, L.Label, Signal[T]) = {
    val state = Var(options.head)
    (select(id, List(Group(None, options.toList)), state.writer, state.signal), label(id), state.signal.map(_.value))
  }

  def apply[T](
    id: String,
    options: List[Opt[T]],
    selection: Var[T]
  ): (L.Select, L.Label) =
    grouped(id, List(Group(None, options)), selection)

  /** A group of options, shown under its heading */
  final case class Group[+T](heading: Option[String], options: List[Opt[T]])

  /** Options shown in groups, each under its heading. Labels must be unique across the groups. */
  def grouped[T](
    id: String,
    groups: List[Group[T]],
    selection: Var[T]
  ): (L.Select, L.Label) = {
    val optMap = groups.flatMap(_.options).map(opt => opt.value -> opt).toMap
    val observer = selection.writer.contramap[Opt[T]](_.value)
    val signal = selection.signal.map(optMap)
    (select(id, groups, observer, signal), label(id))
  }

  private def select[T](
    id: String,
    groups: List[Group[T]],
    selectionObserver: Observer[Opt[T]],
    selectionSignal: Signal[Opt[T]]
  ): L.Select = {
    val options = groups.flatMap(_.options)

    L.select(
      L.idAttr(id),
      L.nameAttr(id),
      L.controlled(
        L.value <-- selectionSignal.map(_.uniqueLabel),
        L.onChange.mapToValue.map(label =>
          options
            .find(_.uniqueLabel == label)
            .getOrElse(throw RuntimeException(s"Unexpected select [$id] option: [$label]"))
        ) --> selectionObserver
      ),
      groups.map {
        case Group(None, options) => options.map(option)
        case Group(Some(heading), options) =>
          List(L.optGroup(L.htmlAttr("label", StringAsIsCodec) := heading, options.map(option)))
      }
    )
  }

  private def option[T](opt: Opt[T]): ReactiveHtmlElement[html.Option] =
    L.option(L.value(opt.uniqueLabel), opt.uniqueLabel)

  private def label(id: String): L.Label =
    L.label(L.forId(id))
}
