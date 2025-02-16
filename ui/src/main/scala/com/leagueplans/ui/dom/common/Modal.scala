package com.leagueplans.ui.dom.common

import com.leagueplans.ui.utils.laminar.LaminarOps.handledAs
import com.raquo.airstream.core.{Observer, Sink}
import com.raquo.airstream.eventbus.{EventBus, WriteBus}
import com.raquo.laminar.api.{L, enrichSource, eventPropToProcessor, seqToModifier}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.HTMLDialogElement

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object Modal {
  final class Controller(underlying: WriteBus[Option[L.Element]]) extends Sink[Option[L.Element]] {
    export underlying.toObserver
    
    def show(content: L.Element): Unit =
      underlying.onNext(Some(content))

    def close(): Unit =
      underlying.onNext(None)
  }
  
  def apply(): (ReactiveHtmlElement[HTMLDialogElement], Modal.Controller) = {
    val content = EventBus[Option[L.Element]]()

    val node =
      L.dialogTag(
        L.cls(Styles.dialog),
        L.div(
          L.cls(Styles.container),
          L.child <-- content.events.map(_.getOrElse(L.emptyNode)),
        ),
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

    (node, Controller(content.writer))
  }

  @js.native @JSImport("/styles/common/modal.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val dialog: String = js.native
    val container: String = js.native
  }
}
