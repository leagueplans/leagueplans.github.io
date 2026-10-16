package com.leagueplans.ui.dom.planning.details.repeat

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, eventPropToProcessor}
import org.scalajs.dom.KeyValue

/** A small box for a condition's number. What's typed is saved on Enter, or on leaving the box, once
  * it can be read. Until then the plan keeps the last number that could be, and [[error]] says why
  * the box's can't. Escape puts the saved number back.
  *
  * @param saved the number as the plan has it, which the box shows while nothing else is typed
  * @param placeholder shown while the box is empty
  * @param blank what Enter in an empty box saves, such as a draft's suggestion
  * @param onEscape told when Escape is pressed, after the box is put back
  */
final class NumberField[T](
  saved: Signal[String],
  placeholder: Signal[String],
  parse: String => Either[String, T],
  onCommit: Observer[T],
  blank: Option[() => T],
  onEscape: () => Unit
) {
  private val typed = Var(Option.empty[String])

  /** Why what's typed can't be saved, if it can't */
  val error: Signal[Option[String]] =
    typed.signal.map(
      _.filterNot(text => blank.nonEmpty && text.trim.isEmpty).flatMap(text => parse(text).left.toOption)
    ).distinct

  def input(label: String, inputMode: String, autofocus: Boolean): L.Input =
    L.input(
      L.typ("text"),
      L.inputMode(inputMode),
      L.aria.label(label),
      L.placeholder <-- placeholder,
      L.value <-- Signal.combine(typed.signal, saved).map((typed, saved) => typed.getOrElse(saved)),
      L.onInput.mapToValue --> (text => typed.set(Some(text))),
      L.aria.invalid <-- error.map(e => if (e.nonEmpty) "true" else "false"),
      L.onKeyDown --> { event =>
        event.key match {
          case KeyValue.Enter =>
            event.preventDefault()
            commit(enter = true)
          case KeyValue.Escape =>
            event.preventDefault()
            event.stopPropagation()
            typed.set(None)
            onEscape()
          case _ => ()
        }
      },
      L.onBlur --> (_ => commit(enter = false)),
      L.onMountCallback[L.Input](ctx => if (autofocus) ctx.thisNode.ref.focus())
    )

  private def commit(enter: Boolean): Unit =
    (typed.now(), blank) match {
      case (None, Some(value)) =>
        if (enter) onCommit.onNext(value())
      case (Some(text), Some(value)) if text.trim.isEmpty =>
        if (enter) {
          typed.set(None)
          onCommit.onNext(value())
        }
      case (Some(text), _) =>
        parse(text).foreach { value =>
          typed.set(None)
          onCommit.onNext(value)
        }
      case (None, None) => ()
    }
}
