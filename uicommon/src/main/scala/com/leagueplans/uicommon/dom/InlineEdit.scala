package com.leagueplans.uicommon.dom

import com.leagueplans.uicommon.utils.laminar.EventProcessorOps.handledWith
import com.raquo.airstream.core.{EventStream, Observer, Signal}
import com.raquo.laminar.api.{L, enrichSource}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** A value shown as a button, which turns into a text box when clicked, to be edited as a
  * [[TextDraft]]
  */
object InlineEdit {
  /** @param value the value being edited
    * @param display the button's contents
    * @param label names the text box for screen readers, which can't see the button it replaces
    * @param toText the text the box starts with for a value
    * @param parse reads the box's text, given the value the edit started from, or explains why
    *              it can't
    * @param onCommit told about each value saved
    * @param onStatus told what the box's text parses to as it's typed, and `None` once the box
    *                 closes, so the caller can show why text can't be saved
    * @param startEditing opens the box without a click
    */
  def apply[T](
    value: Signal[T],
    display: Signal[L.Modifier[L.HtmlElement]],
    label: Signal[String],
    toText: T => String,
    parse: (T, String) => Either[String, T],
    onCommit: Observer[T],
    onStatus: Observer[Option[Either[String, T]]] = Observer.empty,
    startEditing: EventStream[Unit] = EventStream.empty,
    placeholder: String = "",
    inputMode: String = "text"
  ): L.Span = {
    val draft = TextDraft(toText, parse, onCommit)

    L.span(
      L.cls(Styles.container),
      L.child <-- draft.isEditing.splitBoolean(
        whenTrue = _ =>
          L.input(
            L.cls(Styles.input),
            L.inputMode(inputMode),
            L.placeholder(placeholder),
            L.aria.label <-- label,
            draft.input
          ),
        whenFalse = _ =>
          Button(_.handledWith(_.sample(value)) --> draft.start).amend(
            L.cls(Styles.button),
            L.child <-- display.map(modifier => L.span(modifier))
          )
      ),
      draft.status --> onStatus,
      L.onUnmountCallback(_ => onStatus.onNext(None)),
      startEditing.sample(value) --> draft.start
    )
  }

  @js.native @JSImport("/styles/common/inlineEdit.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val container: String = js.native
    val button: String = js.native
    val input: String = js.native
  }
}
