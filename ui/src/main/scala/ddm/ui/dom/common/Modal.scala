package ddm.ui.dom.common

import com.raquo.airstream.core.Observer
import com.raquo.airstream.eventbus.{EventBus, WriteBus}
import com.raquo.laminar.api.{L, enrichSource}
import com.raquo.laminar.builders.HtmlTag
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.HTMLDialogElement

object Modal {
  // Currently does not exist in Laminar
  private val dialog: HtmlTag[HTMLDialogElement] =
    L.customHtmlTag("dialog")

  def apply(): (ReactiveHtmlElement[HTMLDialogElement], WriteBus[Option[L.Element]]) = {
    val content = new EventBus[Option[L.Element]]

    val node =
      dialog(
        L.child <-- content.events.map(_.getOrElse(L.emptyNode)),
        L.inContext(node =>
          content.events.map(_.nonEmpty) --> Observer[Boolean] {
            case true => if (!node.ref.open) node.ref.showModal()
            case false => if (node.ref.open) node.ref.close()
          }
        )
      )

    (node, content.writer)
  }
}
