package com.leagueplans.uicommon.dom

import com.leagueplans.uicommon.utils.laminar.EventProcessorOps.handledWith
import com.raquo.airstream.core.{EventStream, Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, enrichSource, eventPropToProcessor}
import org.scalajs.dom.KeyValue

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** A value shown as a button, which turns into a text box when clicked. Enter or leaving the box
  * saves the text, if it parses; Escape cancels.
  */
object InlineEdit {
  /** @param value the value being edited
    * @param display the button's contents for a value
    * @param toText the text the box starts with for a value
    * @param parse reads the box's text, or explains why it can't
    * @param onCommit told about each value saved
    * @param startEditing opens the box without a click
    */
  def apply[T](
    value: Signal[T],
    display: Signal[L.Modifier[L.HtmlElement]],
    toText: T => String,
    parse: String => Either[String, T],
    onCommit: Observer[T],
    startEditing: EventStream[Unit] = EventStream.empty,
    placeholder: String = "",
    inputMode: String = "text"
  ): L.Span = {
    val draft = Var(Option.empty[String])
    val open = Observer[T](current => draft.set(Some(toText(current))))

    L.span(
      L.cls(Styles.container),
      L.child <-- draft.signal.map(_.isDefined).distinct.splitBoolean(
        whenTrue = _ => toInput(draft, parse, onCommit, placeholder, inputMode),
        whenFalse = _ =>
          Button(_.handledWith(_.sample(value)) --> open).amend(
            L.cls(Styles.button),
            L.child <-- display.map(modifier => L.span(modifier))
          )
      ),
      startEditing.sample(value) --> open
    )
  }

  private def toInput[T](
    draft: Var[Option[String]],
    parse: String => Either[String, T],
    onCommit: Observer[T],
    placeholder: String,
    inputMode: String
  ): L.Input = {
    val parsed = draft.signal.map(_.map(parse))

    def finish(save: Boolean): Unit = {
      if (save)
        draft.now().map(parse).foreach(_.foreach(onCommit.onNext))
      draft.set(None)
    }

    L.input(
      L.cls(Styles.input),
      L.typ("text"),
      L.inputMode(inputMode),
      L.placeholder(placeholder),
      L.value <-- draft.signal.map(_.getOrElse("")),
      L.onInput.mapToValue --> (text => draft.set(Some(text))),
      L.aria.invalid <-- parsed.map(p => if (p.exists(_.isLeft)) "true" else "false"),
      L.title <-- parsed.map(_.flatMap(_.left.toOption).getOrElse("")),
      L.onKeyDown --> { event =>
        event.key match {
          case KeyValue.Enter =>
            event.preventDefault()
            if (draft.now().map(parse).exists(_.isRight)) finish(save = true)
          case KeyValue.Escape =>
            event.preventDefault()
            event.stopPropagation()
            finish(save = false)
          case _ => ()
        }
      },
      // Leaving the box saves it, unless it can't be read, in which case the change is dropped
      L.onBlur --> (_ => if (draft.now().isDefined) finish(save = true)),
      L.onMountCallback { ctx =>
        ctx.thisNode.ref.focus()
        ctx.thisNode.ref.select()
      }
    )
  }

  @js.native @JSImport("/styles/common/inlineEdit.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val container: String = js.native
    val button: String = js.native
    val input: String = js.native
  }
}
