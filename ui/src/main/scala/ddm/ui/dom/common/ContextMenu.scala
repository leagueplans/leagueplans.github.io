package ddm.ui.dom.common

import com.raquo.airstream.core.{EventStream, Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.domtypes.jsdom.defs.events.TypedTargetMouseEvent
import com.raquo.laminar.api.{L, StringValueMapper, enrichSource, eventPropToProcessor, styleToReactiveStyle}
import com.raquo.laminar.modifiers.Binder
import com.raquo.laminar.nodes.ReactiveElement.Base
import com.raquo.laminar.nodes.ReactiveHtmlElement
import ddm.ui.facades.dom.RichElement.Ops
import ddm.ui.utils.laminar.LaminarOps.RichL
import org.scalajs.dom.html.Div
import org.scalajs.dom.{Element, MouseEvent}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object ContextMenu {
  type CloseCommand = Any

  final class Controller(
    opener: Observer[(MouseEvent, Option[L.Child])],
    closer: Observer[CloseCommand]
  ) {
    def bind(toContents: Observer[CloseCommand] => Signal[Option[L.Child]]): Binder[Base] = {
      val contents = toContents(closer)
      L.ifUnhandledF(L.onContextMenu)(_.withCurrentValueOf(contents)) --> opener
    }
  }

  def apply(): (ReactiveHtmlElement[Div], Controller) = {
    val status = Var[Status](Status.Closed)
    val controller = new Controller(
      opener(status.writer),
      closer = status.writer.contramap(_ => Status.Closed)
    )
    (toMenu(status), controller)
  }

  sealed trait Status
  object Status {
    private[ContextMenu] final case class Open(x: Double, y: Double, contents: L.Child) extends Status
    case object Closed extends Status
  }

  @js.native @JSImport("/styles/common/contextMenu.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val open: String = js.native
    val closed: String = js.native
  }

  private def opener(status: Observer[Status.Open]): Observer[(MouseEvent, Option[L.Child])] =
    status.contracollect[(MouseEvent, Option[L.Child])] { case (event, Some(contents)) =>
      event.preventDefault()
      Status.Open(event.pageX, event.pageY, contents)
    }

  private def toMenu(status: Var[Status]): ReactiveHtmlElement[Div] =
    L.div(
      L.cls <-- status.signal.map {
        case _: Status.Open => Styles.open
        case Status.Closed => Styles.closed
      },
      L.left <-- toCoords(status.signal)(_.x),
      L.top <-- toCoords(status.signal)(_.y),
      L.child <-- status.signal.map {
        case open: Status.Open => open.contents
        case Status.Closed => L.emptyNode
      },
      closeOnClickOutside(status.writer),
      closeIfAnotherMenuIsOpened(status.writer)
    )

  private def toCoords(status: Signal[Status])(pick: Status.Open => Double): EventStream[String] =
    status.changes.collect { case open: Status.Open => s"${pick(open)}px" }

  private def closeOnClickOutside(status: Observer[Status.Closed.type]): Binder[Base] =
    L.documentEvents
      .onContextMenu
      .filter(!_.defaultPrevented) --> status.contracollect[Any](_ => Status.Closed)

  private def closeIfAnotherMenuIsOpened(status: Observer[Status.Closed.type]): L.Modifier[Base] =
    L.inContext(node =>
      L.documentEvents.onClick --> status.contracollect[TypedTargetMouseEvent[Element]] {
        case event if !event.target.closestClass(Styles.open).contains(node.ref) => Status.Closed
      }
    )
}
