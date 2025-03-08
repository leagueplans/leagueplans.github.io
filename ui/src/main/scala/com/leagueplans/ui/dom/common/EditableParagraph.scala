package com.leagueplans.ui.dom.common

import com.leagueplans.ui.utils.laminar.EventProcessorOps.handled
import com.leagueplans.ui.utils.laminar.EventPropOps.handledAs
import com.leagueplans.ui.utils.laminar.LaminarOps.onKey
import com.raquo.airstream.core.Signal
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.features.unitArrows
import com.raquo.laminar.api.{L, eventPropToProcessor, seqToModifier, textToTextNode}
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
          L.eventProp[InputEvent]("beforeinput")
            .filter(_.inputType == InputType.insertParagraph)
            .filterByTarget(_ == ctx.ref)
            .handled --> ctx.ref.blur(),
          L.onKey(KeyValue.Escape).handled --> ctx.ref.blur()
        ))
      )

    (paragraph, content.signal)
  }
}
