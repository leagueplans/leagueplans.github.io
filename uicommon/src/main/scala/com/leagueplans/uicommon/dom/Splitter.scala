package com.leagueplans.uicommon.dom

import com.raquo.airstream.core.Observer
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, eventPropToProcessor}
import org.scalajs.dom.Element

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** A handle on the left edge of a column, which resizes the column when dragged. The column
  * must be positioned (e.g. `position: relative`), since the handle is placed against its edge.
  * Dragging left widens the column.
  */
object Splitter {
  /** @param currentWidth the column's width when a drag starts
    * @param onResize the width a drag asks for, before any limits are applied
    * @param onRelease told when a drag ends
    */
  def apply(
    currentWidth: () => Int,
    onResize: Observer[Int],
    onRelease: Observer[Unit]
  ): L.Div = {
    val drag = Var(Option.empty[(startX: Double, startWidth: Int)])

    L.div(
      L.cls(Styles.splitter),
      L.cls(Styles.dragging) <-- drag.signal.map(_.isDefined),
      L.role("separator"),
      L.aria.orientation("vertical"),
      L.onPointerDown.filter(_.button == 0) --> { event =>
        event.preventDefault()
        event.target.asInstanceOf[Element].setPointerCapture(event.pointerId)
        drag.set(Some((event.clientX, currentWidth())))
      },
      L.onPointerMove.compose(_.withCurrentValueOf(drag.signal)) --> {
        case (event, Some((startX, startWidth))) =>
          onResize.onNext(startWidth + (startX - event.clientX).round.toInt)
        case (_, None) =>
          ()
      },
      L.onPointerUp.mapToUnit --> (_ => release(drag, onRelease)),
      L.onPointerCancel.mapToUnit --> (_ => release(drag, onRelease))
    )
  }

  private def release(
    drag: Var[Option[(startX: Double, startWidth: Int)]],
    onRelease: Observer[Unit]
  ): Unit =
    if (drag.now().nonEmpty) {
      drag.set(None)
      onRelease.onNext(())
    }

  @js.native @JSImport("/styles/common/splitter.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val splitter: String = js.native
    val dragging: String = js.native
  }
}
