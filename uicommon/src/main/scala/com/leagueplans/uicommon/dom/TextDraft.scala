package com.leagueplans.uicommon.dom

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, eventPropToProcessor, seqToModifier}
import org.scalajs.dom.KeyValue

/** Editing a value as text in a box, which callers show while [[isEditing]].
  *
  * Enter saves the text if it parses, and otherwise keeps the box open. Leaving the box saves the
  * text if it parses, and otherwise discards it. Escape cancels.
  *
  * @param toText the text the box starts with for a value
  * @param parse reads the box's text, given the value the edit started from, or explains why
  *              it can't
  * @param onCommit told about each value saved
  */
final class TextDraft[T](
  toText: T => String,
  parse: (T, String) => Either[String, T],
  onCommit: Observer[T]
) {
  private val draft = Var(Option.empty[(original: T, text: String)])

  val isEditing: Signal[Boolean] =
    draft.signal.map(_.isDefined).distinct

  /** What the box's text parses to while editing, so callers can show why it can't be saved */
  val status: Signal[Option[Either[String, T]]] =
    draft.signal.map(_.map(parseDraft))

  /** Starts editing the value, unless it's already being edited */
  val start: Observer[T] =
    Observer(value => if (draft.now().isEmpty) draft.set(Some((value, toText(value)))))

  /** Replaces the box's text, such as with an example the user picked */
  val setText: Observer[String] =
    Observer(text => draft.update(_.map(d => (d.original, text))))

  /** Makes a text box show and edit the draft, and take the focus when it appears */
  val input: L.Modifier[L.Input] =
    List(
      L.typ("text"),
      L.value <-- draft.signal.map(_.map(_.text).getOrElse("")),
      L.onInput.mapToValue --> setText,
      L.aria.invalid <-- status.map(s => if (s.exists(_.isLeft)) "true" else "false"),
      L.onKeyDown --> { event =>
        event.key match {
          case KeyValue.Enter =>
            event.preventDefault()
            event.stopPropagation()
            if (draft.now().map(parseDraft).exists(_.isRight)) finish(save = true)
          case KeyValue.Escape =>
            event.preventDefault()
            event.stopPropagation()
            finish(save = false)
          case _ => ()
        }
      },
      L.onBlur --> (_ => finish(save = true)),
      L.onMountCallback[L.Input] { ctx =>
        ctx.thisNode.ref.focus()
        ctx.thisNode.ref.select()
      }
    )

  private def parseDraft(d: (original: T, text: String)): Either[String, T] =
    parse(d.original, d.text)

  /** Saves the text if asked to and it parses, then stops editing */
  private def finish(save: Boolean): Unit = {
    if (save) draft.now().map(parseDraft).foreach(_.foreach(onCommit.onNext))
    draft.set(None)
  }
}
