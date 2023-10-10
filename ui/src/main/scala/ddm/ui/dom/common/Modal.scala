package ddm.ui.dom.common

import com.raquo.airstream.core.Observer
import com.raquo.airstream.eventbus.{EventBus, WriteBus}
import com.raquo.laminar.api.{L, enrichSource, eventPropToProcessor, seqToModifier}
import com.raquo.laminar.builders.HtmlTag
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.{Event, HTMLDialogElement, MouseEvent}
import ddm.ui.utils.laminar.LaminarOps.RichL

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object Modal {
  // Currently does not exist in Laminar
  private val dialog: HtmlTag[HTMLDialogElement] =
    L.customHtmlTag("dialog")

  def apply(): (ReactiveHtmlElement[HTMLDialogElement], WriteBus[Option[L.Element]]) = {
    val content = new EventBus[Option[L.Element]]

    val node =
      dialog(
        L.cls(Styles.dialog),
        L.div(
          L.cls(Styles.container),
          L.child <-- content.events.map(_.getOrElse(L.emptyNode)),
        ),
        L.inContext(node =>
          List(
            content.events.map(_.nonEmpty) --> Observer[Boolean] {
              case true => if (!node.ref.open) node.ref.showModal()
              case false => if (node.ref.open) node.ref.close()
            },
            L.ifUnhandledF(L.onClick)(_.filter(_.target == node.ref)) --> content.writer.contramap[MouseEvent](_ => None)
          )
        ),
        L.customEventProp("close") --> content.writer.contramap[Event](_ => None)
      )

    (node, content.writer)
  }

  @js.native @JSImport("/styles/common/modal.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val dialog: String = js.native
    val container: String = js.native
  }
}
