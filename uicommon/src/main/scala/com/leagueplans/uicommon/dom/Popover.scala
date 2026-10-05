package com.leagueplans.uicommon.dom

import com.leagueplans.uicommon.facades.floatingui.{Boundary, FlipOptions, OffsetOptions, Placement, ShiftOptions}
import com.leagueplans.uicommon.utils.laminar.LaminarOps.onKey
import com.leagueplans.uicommon.wrappers.floatingui.{Floating, FloatingConfig}
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, enrichSource, eventPropToProcessor}
import org.scalajs.dom.{Element, KeyValue}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** A card anchored to an element, such as the card that opens beside an item. One card is open
  * at a time. A card closes when Esc is pressed, when something outside it is clicked, or when
  * its anchor is removed from the page.
  */
object Popover {
  def apply(): (L.Div, Popover) = {
    val status = Var(Option.empty[Open])
    val controller = new Popover(status)

    val container =
      L.div(
        L.cls(Styles.container),
        L.child.maybe <-- status.signal.map(_.map(_.card))
      )

    (container, controller)
  }

  private final case class Open(anchor: Element, card: L.HtmlElement)

  @js.native @JSImport("/styles/common/popover.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val container: String = js.native
    val card: String = js.native
  }
}

final class Popover private[Popover](status: Var[Option[Popover.Open]]) {
  /** Opens a card beside the anchor: to its right when there's room within the boundary, and
    * otherwise to its left. Cards for wide anchors, such as the rows of a list, should open below
    * them instead, against their right edge.
    *
    * Opening is deferred so that, when called from a click handler, the card's listener for
    * clicks outside it doesn't see the click that opened it.
    *
    * @param boundary the area the card should prefer to stay within, such as the column the
    *                 anchor sits in, so that the card doesn't cover the rest of the page
    * @param below whether to open below the anchor rather than beside it
    */
  def open(anchor: Element, contents: L.HtmlElement, boundary: Option[Element] = None, below: Boolean = false): Unit =
    js.timers.setTimeout(0)(
      status.set(Some(Popover.Open(anchor, decorate(anchor, contents, boundary, below))))
    ): Unit

  def close(): Unit =
    status.set(None)

  def isAnchoredTo(element: Element): Signal[Boolean] =
    status.signal.map(_.exists(_.anchor == element)).distinct

  /** Add to anything a card can be anchored to, so that its card closes when it's removed */
  val closesWithAnchor: L.Modifier[L.Element] =
    L.onUnmountCallback(element =>
      if (status.now().exists(_.anchor == element.ref)) close()
    )

  private def decorate(anchor: Element, contents: L.HtmlElement, maybeBoundary: Option[Element], below: Boolean): L.HtmlElement = {
    val flipBoundary: js.UndefOr[Boundary] = maybeBoundary match {
      case Some(element) => element
      case None => js.undefined
    }
    val config =
      FloatingConfig(
        placement = Some(if (below) Placement.bottomEnd else Placement.rightStart),
        offset = Some(new OffsetOptions { mainAxis = if (below) 4 else 8 }),
        flip = Some(new FlipOptions {
          fallbackPlacements = js.Array(if (below) Placement.topEnd else Placement.leftStart)
          boundary = flipBoundary
        }),
        shift = Some(new ShiftOptions { padding = 8 }),
        fadeIn = false
      )

    contents.amend(
      L.cls(Popover.Styles.card),
      Floating.anchorTo(anchor, config),
      closeOnClickOutside(anchor),
      L.documentEvents(_.onKey(KeyValue.Escape)) --> Observer[Any](_ => close())
    )
  }

  // Clicks on the anchor are left to the anchor, which may toggle its card
  private def closeOnClickOutside(anchor: Element): L.Modifier[L.Element] =
    L.inContext(node =>
      L.documentEvents(_.onMouseDown) --> Observer[org.scalajs.dom.MouseEvent](event =>
        event.target match {
          case target: Element if node.ref.contains(target) || anchor.contains(target) => ()
          case _ => close()
        }
      )
    )
}
