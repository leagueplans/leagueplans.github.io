package com.leagueplans.ui.dom.common.form

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.{StrictSignal, Var}
import com.raquo.laminar.api.{L, enrichSource, eventPropToProcessor, seqToModifier}
import org.scalajs.dom.Event

object RadioGroup {
  final case class Opt[T](value: T, id: String)

  // autoSelected indicates that the user did not explicitly choose this option, so it's
  // acceptable to modify the selection without user input
  private final case class Selection[T](opt: Opt[T], autoSelected: Boolean)

  /** The first option is chosen as the default */
  def apply[T](
    groupName: String,
    default: Opt[T],
    options: List[Opt[T]],
    render: (T, Signal[Boolean], L.Input, L.Label) => List[L.Node]
  ): (List[L.Node], Signal[T]) = {
    val selection = Var(Selection(default, autoSelected = true))
    val rendering =
      options.flatMap { opt =>
        val checked = selection.signal.map(_.opt == opt)
        val selector = selection.writer.contramap[Any](_ => Selection(opt, autoSelected = false))
        option(opt, groupName, checked, selector, render)
      }

    (rendering, selection.signal.map(_.opt.value))
  }

  /** Manual selections are maintained so long as they remain in the
    * list of options. If a manual selection has not been made, then
    * the first item in the list will be chosen as the default. */
  def apply[T](
    groupName: String,
    options: Signal[List[Opt[T]]],
    render: (T, Signal[Boolean], L.Input, L.Label) => List[L.Node]
  ): (L.Modifier[L.Element], Signal[Option[T]]) = {
    val selection = Var[Option[Selection[T]]](None)
    val rendering =
      options.split(_.id) { (_, opt, _) =>
        val checked = selection.signal.map(_.exists(_.opt == opt))
        val selector = selection.writer.contramap[Any](_ => Some(Selection(opt, autoSelected = false)))
        option(opt, groupName, checked, selector, render)
      }.map(_.flatten)

    val bindings =
      List(
        options --> updateDefaultSelection(selection),
        L.children <-- rendering
      )

    (bindings, selection.signal.map(_.map(_.opt.value)))
  }
  
  def apply[T](
    groupName: String,
    options: List[Opt[T]],
    externalSignal: StrictSignal[T],
    externalConsumer: Observer[T],
    render: (T, Signal[Boolean], L.Input, L.Label) => List[L.Node]
  ): List[L.Modifier[L.Element]] =
    options.flatMap { opt =>
      val checked = externalSignal.map(_ == opt.value)
      val selector = externalConsumer.contramap[Any](_ => opt.value)
      option(opt, groupName, checked, selector, render)
    }

  private def option[T](
    opt: Opt[T],
    groupName: String,
    checked: Signal[Boolean],
    selector: Observer[Event],
    render: (T, Signal[Boolean], L.Input, L.Label) => List[L.Node]
  ): List[L.Node] = {
    val id = s"$groupName-${opt.id}"
    render(
      opt.value,
      checked,
      radio(id, groupName, checked, selector),
      label(id)
    )
  }

  private def radio(
    id: String,
    groupName: String,
    checked: Signal[Boolean],
    selector: Observer[Event]
  ): L.Input =
    L.input(
      L.`type`("radio"),
      L.idAttr(id),
      L.nameAttr(groupName),
      L.controlled(
        L.checked <-- checked,
        L.onClick --> selector
      )
    )

  private def label(id: String): L.Label =
    L.label(L.forId(id))

  private def updateDefaultSelection[T](
    selection: Var[Option[Selection[T]]]
  ): Observer[List[Opt[T]]] =
    selection.updater[List[Opt[T]]] {
      case (Some(selection), updatedOptions) if !selection.autoSelected && updatedOptions.contains(selection.opt) =>
        Some(selection)
      case (_, default :: _) =>
        Some(Selection(default, autoSelected = true))
      case (_, Nil) =>
        None
    }
}
