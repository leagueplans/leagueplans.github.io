package com.leagueplans.ui.dom.common

import com.leagueplans.ui.utils.laminar.EventProcessorOps.handledAs
import com.raquo.airstream.core.{Observer, Signal, Sink}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, enrichSource, eventPropToProcessor, seqToModifier}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html.Dialog

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object Modal {
  def apply(popovers: L.Element): (ReactiveHtmlElement[Dialog], Modal) = {
    val contents = Var(Option.empty[L.Element])

    val element =
      L.dialogTag(
        L.cls(Styles.dialog),
        L.div(
          L.cls(Styles.contents),
          L.child.maybe <-- contents,
        ),
        // Ordering is important here. The popovers may steal focus from the modal contents
        // on mounting if they are listed first
        L.child.maybe <-- contents.signal.map(_.map(_ => popovers)),
        L.inContext(node =>
          List(
            contents.signal.map(_.nonEmpty) --> {
              case true => if (!node.ref.open) node.ref.showModal()
              case false => if (node.ref.open) node.ref.close()
            },
            L.onClick.filterByTarget(_ == node.ref).handledAs(None) --> contents
          )
        ),
        L.eventProp("close").mapToStrict(None) --> contents
      )

    (element, new Modal(contents))
  }

  @js.native @JSImport("/styles/common/modal.module.css", JSImport.Default)
  object Styles extends js.Object {
    val dialog: String = js.native
    val contents: String = js.native

    val form: String = js.native
    val title: String = js.native
    
    val button: String = js.native
    val confirmationButton: String = js.native
    val deletionButton: String = js.native
  }
}

final class Modal private(contents: Var[Option[L.Element]]) extends Sink[Option[L.Element]] {
  export contents.toObserver

  def show(content: L.Element): Unit =
    contents.set(Some(content))

  def close(): Unit =
    contents.set(None)

  val isOpen: Signal[Boolean] =
    contents.signal.map(_.isDefined).distinct
}
