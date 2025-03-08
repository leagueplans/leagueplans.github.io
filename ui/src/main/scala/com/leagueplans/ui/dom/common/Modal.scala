package com.leagueplans.ui.dom.common

import com.leagueplans.ui.utils.laminar.EventProcessorOps.handledAs
import com.raquo.airstream.core.{Observer, Sink}
import com.raquo.airstream.eventbus.{EventBus, WriteBus}
import com.raquo.laminar.api.{L, enrichSource, eventPropToProcessor, seqToModifier}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html.Dialog

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object Modal {
  def apply(): (ReactiveHtmlElement[Dialog], Modal) = {
    val content = EventBus[Option[L.Element]]()

    val element =
      L.dialogTag(
        L.cls(Styles.dialog),
        L.child.maybe <-- content.events,
        L.inContext(node =>
          List(
            content.events.map(_.nonEmpty) --> {
              case true => if (!node.ref.open) node.ref.showModal()
              case false => if (node.ref.open) node.ref.close()
            },
            L.onClick.filterByTarget(_ == node.ref).handledAs(None) --> content
          )
        ),
        L.eventProp("close").mapToStrict(None) --> content
      )

    (element, new Modal(content.writer))
  }

  @js.native @JSImport("/styles/common/modal.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val dialog: String = js.native
  }
}

final class Modal private(underlying: WriteBus[Option[L.Element]]) extends Sink[Option[L.Element]] {
  export underlying.toObserver

  def show(content: L.Element): Unit =
    underlying.onNext(Some(content))

  def close(): Unit =
    underlying.onNext(None)
}
