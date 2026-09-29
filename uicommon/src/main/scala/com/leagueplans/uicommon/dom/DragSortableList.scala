package com.leagueplans.uicommon.dom

import com.leagueplans.uicommon.facades.fontawesome.freesolid.FreeSolid
import com.leagueplans.uicommon.utils.HasID
import com.leagueplans.uicommon.utils.airstream.ObservableOps.unzip
import com.leagueplans.uicommon.utils.laminar.EventProcessorOps.handledWith
import com.leagueplans.uicommon.utils.laminar.EventPropOps.ifUnhandled
import com.leagueplans.uicommon.utils.laminar.FontAwesome
import com.raquo.airstream.core.{EventStream, Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, eventPropToProcessor, seqToModifier}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html.OList
import org.scalajs.dom.{DataTransferDropEffectKind, DataTransferEffectAllowedKind, DragEvent}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** A list that can be reordered by dragging its items.
  *
  * While an item is being dragged, the list shows a preview of the new order. The new order is
  * only reported once the item is dropped, and not at all if the drag is cancelled. */
object DragSortableList {
  def apply[T : HasID as hasID](
    id: String,
    orderSignal: Signal[List[T]],
    orderObserver: Observer[List[T]],
    toElement: (hasID.ID, T, Signal[T], L.SvgElement) => L.Modifier[L.HtmlElement]
  ): ReactiveHtmlElement[OList] = {
    val eventFormat = s"application/listitem;id=$id"
    val dragTracker = Var[Option[Dragging[hasID.ID, T]]](None).distinct
    val displayedOrder =
      Signal.combine(orderSignal, dragTracker.signal).map {
        case (_, Some(dragging)) => dragging.previewOrder
        case (order, None) => order
      }

    val children =
      displayedOrder
        .map(_.zipWithIndex)
        .split((data, _) => data.id) { case (itemID, (data, _), zippedSignal) =>
          val (dataSignal, indexSignal) = zippedSignal.unzip
          val (icon, draggableSignal) = dragIcon

          L.li(
            toElement(itemID, data, dataSignal, icon),
            L.draggable <-- draggableSignal,
            onDragStart(eventFormat, itemID, indexSignal, displayedOrder, dragTracker.writer),
            onDragInto(itemID, dragTracker, indexSignal),
            onDragEnd(dragTracker, orderObserver)
          )
        }

    L.ol(
      L.cls(Styles.list),
      L.children <-- children
    )
  }

  @js.native @JSImport("/styles/common/dragSortableList.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val list: String = js.native
    val icon: String = js.native
  }

  private final case class Dragging[ID, T](
    id: ID,
    originalIndex: Int,
    originalOrder: List[T],
    previewOrder: List[T]
  )

  private def dragIcon: (L.SvgElement, Signal[Boolean]) = {
    val mouseOver = Var(false)
    val icon = FontAwesome.icon(FreeSolid.faGripVertical).amend(
      L.svg.cls(Styles.icon),
      L.onMouseOver.mapToStrict(true) --> mouseOver,
      L.onMouseLeave.mapToStrict(false) --> mouseOver
    )
    (icon, mouseOver.signal)
  }

  private def onDragStart[ID, T](
    eventFormat: String,
    itemID: ID,
    itemIndex: Signal[Int],
    order: Signal[List[T]],
    dragTracker: Observer[Option[Dragging[ID, T]]]
  ): L.Modifier[L.HtmlElement] =
    L.inContext(ctx =>
      L.onDragStart.compose(
        // Can't use preventDefault here, since it stops the browser from
        // actually dragging the element
        _.filter(_.target == ctx.ref)
          .withCurrentValueOf(itemIndex, order)
      ) -->
        dragTracker.contramap[(DragEvent, Int, List[T])] { (event, originalIndex, originalOrder) =>
          // We don't use this, but it informs other apps not to receive the drop
          event.dataTransfer.setData(eventFormat, "placeholder")
          event.dataTransfer.effectAllowed = DataTransferEffectAllowedKind.move
          Some(Dragging(itemID, originalIndex, originalOrder, previewOrder = originalOrder))
        }
    )

  /** Update the previewed order */
  private def onDragInto[ID, T](
    itemID: ID,
    dragTracker: Var[Option[Dragging[ID, T]]],
    indexSignal: Signal[Int]
  ): L.Modifier[L.HtmlElement] = {
    val streamMutator: EventStream[DragEvent] => EventStream[(Dragging[ID, T], Int)] =
      _.withCurrentValueOf(dragTracker.signal, indexSignal)
        .collect(Function.unlift {
          case (event, Some(dragging), index) =>
            event.preventDefault()
            Option.when(dragging.id != itemID)((dragging, index))
          case _ =>
            None
        })

    val previewMutator =
      dragTracker.writer.contramap[(Dragging[ID, T], Int)]((dragging, index) =>
        Some(dragging.copy(previewOrder =
          move(
            dragging.originalOrder,
            from = dragging.originalIndex,
            to = index
          )
        ))
      )

    List(
      L.onDragEnter.ifUnhandled.compose(streamMutator) --> previewMutator,
      L.onDragOver.ifUnhandled.compose(streamMutator) --> previewMutator
    )
  }

  /** Report the previewed order if the item was dropped, then stop previewing */
  private def onDragEnd[ID, T](
    dragTracker: Var[Option[Dragging[ID, T]]],
    orderObserver: Observer[List[T]]
  ): L.Modifier[L.HtmlElement] = {
    // The new order is reported before the preview is cleared, so that the list doesn't
    // briefly show the original order
    val observer = Observer.combine(
      orderObserver.contracollect[(DragEvent, Option[Dragging[ID, T]])] {
        case (event, Some(dragging))
          if event.dataTransfer.dropEffect != DataTransferDropEffectKind.none &&
            dragging.previewOrder != dragging.originalOrder =>
          dragging.previewOrder
      },
      dragTracker.writer.contramap[Any](_ => None)
    )

    L.inContext(ctx =>
      L.onDragEnd
        .filterByTarget(_ == ctx.ref)
        .handledWith(_.withCurrentValueOf(dragTracker.signal)) --> observer
    )
  }

  private def move[T](order: List[T], from: Int, to: Int): List[T] = {
    val buffer = order.toBuffer
    val data = buffer.remove(from)
    buffer.insert(to, data)
    buffer.toList
  }
}
