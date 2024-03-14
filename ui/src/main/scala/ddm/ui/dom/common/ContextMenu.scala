package ddm.ui.dom.common

import com.raquo.airstream.core.{EventStream, Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, StringValueMapper, enrichSource, eventPropToProcessor}
import com.raquo.laminar.modifiers.Binder
import com.raquo.laminar.nodes.ReactiveElement.Base
import com.raquo.laminar.nodes.ReactiveHtmlElement
import ddm.ui.utils.laminar.LaminarOps.*
import org.scalajs.dom.html.Div
import org.scalajs.dom.{Element, MouseEvent}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object ContextMenu {
  type CloseCommand = Any

  final class Controller(
    opener: Observer[(MouseEvent, Option[L.Node])],
    closer: Observer[CloseCommand]
  ) {
    def bind(toContents: Observer[CloseCommand] => Signal[Option[L.Node]]): Binder[Base] = {
      val contents = toContents(closer)
      L.onContextMenu.ifUnhandledF(_.withCurrentValueOf(contents)) --> opener
    }
  }

  def apply(): (ReactiveHtmlElement[Div], Controller) = {
    val status = Var[Status](Status.Closed)
    val controller = Controller(
      opener(status.writer),
      closer = status.writer.contramap(_ => Status.Closed)
    )
    (toMenu(status), controller)
  }

  private enum Status {
    case Open(x: Double, y: Double, contents: L.Node)
    case Closed
  }

  @js.native @JSImport("/styles/common/contextMenu.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val open: String = js.native
    val closed: String = js.native
  }

  private def opener(status: Observer[Status.Open]): Observer[(MouseEvent, Option[L.Node])] =
    status.contracollect[(MouseEvent, Option[L.Node])] { case (event, Some(contents)) =>
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

  private def closeOnClickOutside(status: Observer[Status]): Binder[Base] =
    L.documentEvents(
      _.onContextMenu
        .filter(!_.defaultPrevented)
        .mapTo(Status.Closed)
    ) --> status

  private def closeIfAnotherMenuIsOpened(status: Observer[Status]): L.Modifier[Base] =
    L.inContext(node =>
      L.documentEvents(
        _.onClick
          .mapToTargetAs[Element]
          .filter(target => !Option(target.closest(s".${Styles.open}")).contains(node.ref))
          .mapTo(Status.Closed)
      ) --> status
    )
}
