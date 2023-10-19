package ddm.ui.dom.common

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, seqToModifier, textToTextNode}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import ddm.ui.utils.laminar.LaminarOps.RichEventProp
import org.scalajs.dom._
import org.scalajs.dom.html.Paragraph

object  EditableParagraph {
  def apply(initial: String): (ReactiveHtmlElement[Paragraph], Signal[String]) = {
    val content = Var(initial)

    val paragraph =
      L.p(
        initial,
        L.contentEditable(true),
        L.inContext(ctx => List(
          L.onInput.handledAs(ctx.ref.innerText) --> content.writer,
          // Kind of hacky... but I want something to trigger blurring and the enter
          // key seems the best choice
          L.eventProp[InputEvent]("beforeinput").ifUnhandled --> Observer[InputEvent](event =>
            if (event.inputType == InputType.insertParagraph && event.target == ctx.ref) {
              event.preventDefault()
              ctx.ref.blur()
            }
          ),
          L.onKeyDown.ifUnhandledF(_.filter(_.keyCode == KeyCode.Escape)) --> Observer[KeyboardEvent] { event =>
            event.preventDefault()
            ctx.ref.blur()
          }
        ))
      )

    (paragraph, content.signal)
  }
}
