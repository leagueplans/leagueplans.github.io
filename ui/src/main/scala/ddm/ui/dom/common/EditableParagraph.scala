package ddm.ui.dom.common

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.domtypes.jsdom.defs.events.TypedTargetEvent
import com.raquo.laminar.api.{L, eventPropToProcessor, seqToModifier, textToNode}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import ddm.ui.utils.laminar.LaminarOps.RichL
import org.scalajs.dom.html.Paragraph
import org.scalajs.dom._

object  EditableParagraph {
  def apply(initial: String): (ReactiveHtmlElement[Paragraph], Signal[String]) = {
    val content = Var(initial)

    val paragraph =
      L.p(
        initial,
        L.contentEditable(true),
        L.ifUnhandled(L.onInput) --> content.writer.contramap[TypedTargetEvent[HTMLElement]] { event =>
          event.preventDefault()
          event.target.innerText
        },
        L.inContext(ctx => List(
          // Kind of hacky... but I want something to trigger blurring and the enter
          // key seems the best choice
          L.ifUnhandled(L.customEventProp[InputEvent]("beforeinput")) --> Observer[InputEvent](event =>
            if (event.inputType == InputType.insertParagraph && event.target == ctx.ref) {
              event.preventDefault()
              ctx.ref.blur()
            }
          ),
          L.ifUnhandledF(L.onKeyDown)(_.filter(_.keyCode == KeyCode.Escape)) --> Observer[KeyboardEvent] { event =>
            event.preventDefault()
            ctx.ref.blur()
          }
        ))
      )

    (paragraph, content.signal)
  }
}
