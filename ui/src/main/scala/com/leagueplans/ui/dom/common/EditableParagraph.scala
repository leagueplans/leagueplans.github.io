package com.leagueplans.ui.dom.common

import com.leagueplans.ui.utils.laminar.LaminarOps.*
import com.raquo.airstream.core.Signal
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, seqToModifier, textToTextNode}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.*
import org.scalajs.dom.html.Paragraph

object  EditableParagraph {
  def apply(initial: String): (ReactiveHtmlElement[Paragraph], Signal[String]) = {
    val content = Var(initial)

    val paragraph =
      L.p(
        initial,
        L.contentEditable(true),
        L.inContext(ctx => List(
          L.onInput.handledAs(ctx.ref.innerText) --> content,
          // Kind of hacky... but I want something to trigger blurring and the enter
          // key seems the best choice
          L.eventProp[InputEvent]("beforeinput").ifUnhandled --> (event =>
            if (event.inputType == InputType.insertParagraph && event.target == ctx.ref) {
              event.preventDefault()
              ctx.ref.blur()
            }
          ),
          L.onKeyDown.ifUnhandledF(_.filter(_.keyCode == KeyCode.Escape)) --> { event =>
            event.preventDefault()
            ctx.ref.blur()
          }
        ))
      )

    (paragraph, content.signal)
  }
}
