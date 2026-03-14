package com.leagueplans.ui.dom.common

import com.leagueplans.ui.dom.common.ContextMenu.Status
import com.leagueplans.ui.utils.laminar.EventProcessorOps.handledWith
import com.leagueplans.ui.utils.laminar.EventPropOps.ifUnhandled
import com.leagueplans.ui.wrappers.floatingui.{Floating, FloatingConfig}
import com.raquo.airstream.core.{EventStream, Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, enrichSource, eventPropToProcessor}
import com.raquo.laminar.modifiers.Binder
import org.scalajs.dom.{Element, MouseEvent}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object ContextMenu {
  def apply(): (L.Div, ContextMenu) = {
    val status = Var[Status](Status.Closed)
    val controller = new ContextMenu(status.writer)
    val container =
      L.div(
        L.cls(Styles.container),
        L.child.maybe <-- status.signal.map {
          case open: Status.Open => Some(open.contents)
          case Status.Closed => None
        }
      )

    (container, controller)
  }

  private enum Status {
    case Open(contents: L.HtmlElement)
    case Closed
  }

  @js.native @JSImport("/styles/common/contextMenu.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val container: String = js.native
    val menu: String = js.native
  }
}

final class ContextMenu private[ContextMenu](status: Observer[Status]) {
  def register(
    makeContents: () => L.HtmlElement,
    config: FloatingConfig = FloatingConfig.basicContextMenu()
  ): Binder.Base =
    L.onContextMenu
      .handledWith(_.map(event =>
        Status.Open(createMenu(makeContents, config, event))
      )) --> status

  def registerConditionally(
    makeContents: Signal[Option[() => L.HtmlElement]]
  )(config: FloatingConfig = FloatingConfig.basicContextMenu()): Binder.Base =
    L.onContextMenu
      .ifUnhandled
      .compose(events =>
        events.withCurrentValueOf(makeContents).collect {
          case (event, Some(makeContents)) =>
            event.preventDefault()
            Status.Open(createMenu(makeContents, config, event))
        }
      ) --> status

  def close(): Unit =
    status.onNext(Status.Closed)

  private def createMenu(
    makeContents: () => L.HtmlElement,
    config: FloatingConfig,
    event: MouseEvent
  ): L.HtmlElement =
    makeContents().amend(
      L.cls(ContextMenu.Styles.menu),
      Floating.anchorTo(event.pageX, event.pageY, config),
      closeOnClickOutside,
      closeIfAnotherMenuIsOpened
    )

  private val closeOnClickOutside: L.Modifier[L.Element] =
    L.inContext(node =>
      L.documentEvents(
        _.onClick.collect(Function.unlift(event =>
          event.target match {
            case target: Element => Option.when(!node.ref.contains(target))(Status.Closed)
            case _ => Some(Status.Closed)
          }
        ))
      ) --> status
    )

  private val closeIfAnotherMenuIsOpened: Binder.Base =
    L.documentEvents(
      _.onContextMenu.ifUnhandled.mapToStrict(Status.Closed)
    ) --> status
}
