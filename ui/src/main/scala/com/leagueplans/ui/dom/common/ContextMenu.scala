package com.leagueplans.ui.dom.common

import com.leagueplans.ui.utils.laminar.EventPropOps.ifUnhandled
import com.raquo.airstream.core.{EventStream, Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, StringValueMapper, enrichSource, eventPropToProcessor}
import com.raquo.laminar.modifiers.Binder
import org.scalajs.dom.Element

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object ContextMenu {
  type CloseCommand = Any

  final class Controller private[ContextMenu](status: Var[Status]) {
    private val closer = status.writer.contramap[CloseCommand](_ => Status.Closed)

    def bind(toContents: Observer[CloseCommand] => Signal[Option[L.Node]]): Binder.Base = {
      val contents = toContents(closer)
      L.onContextMenu
        .ifUnhandled
        .compose(events =>
          events.withCurrentValueOf(contents).collect {
            case (event, Some(contents)) =>
              event.preventDefault()
              Status.Open(event.pageX, event.pageY, contents)
          }
        ) --> status
    }
  }

  def apply(): (L.Div, Controller) = {
    val status = Var[Status](Status.Closed)
    val controller = Controller(status)
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

  private def toMenu(status: Var[Status]): L.Div =
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
    status.changes.collect { case open: Status.Open => L.style.px(pick(open).toInt) }

  private def closeOnClickOutside(status: Observer[Status]): L.Modifier[L.Element] =
    L.inContext(node =>
      L.documentEvents(
        _.onClick
          .mapToTargetAs[Element]
          .filter(target => !Option(target.closest(s".${Styles.open}")).contains(node.ref))
          .mapToStrict(Status.Closed)
      ) --> status
    )

  private def closeIfAnotherMenuIsOpened(status: Observer[Status]): Binder.Base =
    L.documentEvents(_.onContextMenu.ifUnhandled.mapToStrict(Status.Closed)) --> status
}
