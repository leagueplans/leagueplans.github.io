package ddm.ui.dom.common

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.domtypes.jsdom.defs.events.TypedTargetMouseEvent
import com.raquo.laminar.api.{L, enrichSource, eventPropToProcessor, seqToModifier, styleToReactiveStyle}
import com.raquo.laminar.nodes.ReactiveElement.Base
import ddm.ui.facades.dom.RichElement.Ops
import org.scalajs.dom.{Element, MouseEvent}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object ContextMenu {
  def apply(constructor: Signal[() => L.HtmlElement]): (L.Modifier[L.HtmlElement], Observer[Status.Closed.type]) = {
    val status = Var[Status](Status.Closed)
    (parentModifier(status, constructor), status.writer)
  }

  sealed trait Status
  object Status {
    private[ContextMenu] final case class Open(x: Double, y: Double) extends Status
    case object Closed extends Status
  }

  @js.native @JSImport("/styles/common/contextMenu.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val hasMenu: String = js.native
    val menu: String = js.native
  }

  private def parentModifier(
    status: Var[Status],
    constructor: Signal[() => L.HtmlElement]
  ): L.Modifier[L.HtmlElement] =
    List(
      L.cls(Styles.hasMenu),
      L.inContext[Base](node =>
        List(
          L.onContextMenu --> listenForOpening(status.writer, node),
          L.child <-- toMenuSignal(status, constructor, node),
        )
      )
    )

  private def listenForOpening(
    status: Observer[Status.Open],
    node: L.Element
  ): Observer[MouseEvent] =
    status.contracollect[MouseEvent] { case event if isClosestListener(event, node.ref, Styles.hasMenu) =>
      event.preventDefault()
      Status.Open(event.pageX, event.pageY)
    }

  private def isClosestListener(event: MouseEvent, parent: Element, cls: String): Boolean =
    event.target.isInstanceOf[Element] &&
      event.target.asInstanceOf[Element]
        .closestClass(cls)
        .contains(parent)

  private def toMenuSignal(
    status: Var[Status],
    constructorSignal: Signal[() => L.HtmlElement],
    parent: L.Element
  ): Signal[L.Node] =
    status.signal.withCurrentValueOf(constructorSignal).map {
      case (Status.Closed, _) => L.emptyNode
      case (open: Status.Open, constructor) => menu(constructor, open, parent, status.writer)
    }

  private def menu(
    constructor: () => L.HtmlElement,
    position: Status.Open,
    parent: L.Element,
    status: Observer[Status.Closed.type]
  ): L.HtmlElement =
    constructor().amend(
      L.cls(Styles.menu),
      L.left(s"${position.x}px"),
      L.top(s"${position.y}px"),
      closeOnOuterClick(status),
      closeOnAnotherMenuOpening(parent, status)
    )

  private def closeOnOuterClick(status: Observer[Status.Closed.type]): L.Modifier[Base] =
    L.inContext(node =>
      L.documentEvents.onClick --> status.contracollect[TypedTargetMouseEvent[Element]] {
        case event if !isClosestListener(event, node.ref, Styles.menu) => Status.Closed
      }
    )

  private def closeOnAnotherMenuOpening(
    parent: L.Element,
    status: Observer[Status.Closed.type]
  ): L.Modifier[Base] =
    L.documentEvents.onContextMenu --> status.contracollect[MouseEvent] {
      case event if !isClosestListener(event, parent.ref, Styles.hasMenu) => Status.Closed
    }
}
