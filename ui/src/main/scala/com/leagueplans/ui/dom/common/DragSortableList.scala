package com.leagueplans.ui.dom.common

import com.leagueplans.ui.facades.fontawesome.freesolid.FreeSolid
import com.leagueplans.ui.utils.HasID
import com.leagueplans.ui.utils.airstream.ObservableOps.unzip
import com.leagueplans.ui.utils.laminar.FontAwesome
import com.leagueplans.ui.utils.laminar.LaminarOps.{ifUnhandledF, handledWith}
import com.raquo.airstream.core.{EventStream, Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, eventPropToProcessor, seqToModifier}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html.OList
import org.scalajs.dom.{DataTransferDropEffectKind, DataTransferEffectAllowedKind, DragEvent}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object DragSortableList {
  def apply[T : HasID as hasID](
    id: String,
    orderSignal: Signal[List[T]],
    orderObserver: Observer[List[T]],
    toElement: (hasID.ID, T, Signal[T], L.SvgElement) => L.Modifier[L.HtmlElement]
  ): ReactiveHtmlElement[OList] = {
    val eventFormat = s"application/listitem;id=$id"
    val dragTracker = Var[Option[Dragging[hasID.ID, T]]](None)

    val children =
      orderSignal
        .map(_.zipWithIndex)
        .split((data, _) => data.id) { case (itemID, (data, _), zippedSignal) =>
          val (dataSignal, indexSignal) = zippedSignal.unzip
          val (icon, draggableSignal) = dragIcon

          L.li(
            toElement(itemID, data, dataSignal, icon),
            L.draggable <-- draggableSignal,
            onDragStart(eventFormat, itemID, indexSignal, orderSignal, dragTracker.writer),
            onDragInto(itemID, dragTracker.signal, indexSignal, orderObserver),
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

  private final case class Dragging[ID, T](id: ID, originalIndex: Int, originalOrder: List[T])

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
          Some(Dragging(itemID, originalIndex, originalOrder))
        }
    )

  /** Update the order */
  private def onDragInto[ID, T](
    itemID: ID,
    dragTracker: Signal[Option[Dragging[ID, T]]],
    indexSignal: Signal[Int],
    orderObserver: Observer[List[T]]
  ): L.Modifier[L.HtmlElement] = {
    val streamMutator: EventStream[DragEvent] => EventStream[(Dragging[ID, T], Int)] =
      _.withCurrentValueOf(dragTracker, indexSignal)
        .collect(Function.unlift {
          case (event, Some(dragging), index) =>
            event.preventDefault()
            Option.when(dragging.id != itemID)((dragging, index))
          case _ =>
            None
        })

    val orderMutator =
      orderObserver.contramap[(Dragging[ID, T], Int)]((dragging, index) =>
        move(
          dragging.originalOrder,
          from = dragging.originalIndex,
          to = index
        )
      )

    List(
      L.onDragEnter.ifUnhandledF(streamMutator) --> orderMutator,
      L.onDragOver.ifUnhandledF(streamMutator) --> orderMutator
    )
  }

  /** Reset the order to the original order if not dropped successfully */
  private def onDragEnd[ID, T](
    dragTracker: Var[Option[Dragging[ID, T]]],
    orderObserver: Observer[List[T]]
  ): L.Modifier[L.HtmlElement] = {
    val observer = Observer.combine(
      dragTracker.writer.contramap[Any](_ => None),
      orderObserver.contracollect[(DragEvent, Option[Dragging[ID, T]])] {
        case (event, Some(dragging)) if event.dataTransfer.dropEffect == DataTransferDropEffectKind.none =>
          dragging.originalOrder
      }
    )

    L.inContext(ctx =>
      L.onDragEnd
        .filterByTarget(_ == ctx.ref)
        .handledWith(_.withCurrentValueOf(dragTracker)) --> observer
    )
  }

  private def move[T](order: List[T], from: Int, to: Int): List[T] = {
    val buffer = order.toBuffer
    val data = buffer.remove(from)
    buffer.insert(to, data)
    buffer.toList
  }
}
