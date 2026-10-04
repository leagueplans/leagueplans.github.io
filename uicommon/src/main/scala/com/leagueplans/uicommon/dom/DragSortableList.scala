package com.leagueplans.uicommon.dom

import com.leagueplans.uicommon.facades.fontawesome.freesolid.FreeSolid
import com.leagueplans.uicommon.utils.HasID
import com.leagueplans.uicommon.utils.airstream.ObservableOps.unzip
import com.leagueplans.uicommon.utils.laminar.EventProcessorOps.handledWith
import com.leagueplans.uicommon.utils.laminar.FontAwesome
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, enrichSource, eventPropToProcessor, seqToModifier}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html.OList
import org.scalajs.dom.{DataTransferDropEffectKind, DataTransferEffectAllowedKind, DragEvent, Node}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** A list that can be reordered by dragging its items.
  *
  * While an item is being dragged, the list stays as it is and a line marks the gap the item
  * would be dropped into. Items of different heights would make a live preview of the new order
  * jump around under the pointer. The new order is only reported once the item is dropped, and
  * not at all if the drag is cancelled. */
object DragSortableList {
  def apply[T : HasID as hasID](
    id: String,
    orderSignal: Signal[List[T]],
    orderObserver: Observer[List[T]],
    toElement: (hasID.ID, T, Signal[T], L.SvgElement) => L.Modifier[L.HtmlElement]
  ): ReactiveHtmlElement[OList] = {
    val eventFormat = s"application/listitem;id=$id"
    val dragTracker = Var[Option[Dragging]](None).distinct

    val children =
      orderSignal
        .map(_.zipWithIndex)
        .split((data, _) => data.id) { case (itemID, (data, _), zippedSignal) =>
          val (dataSignal, indexSignal) = zippedSignal.unzip
          val (icon, draggableSignal) = dragIcon
          val isLast = Signal.combine(indexSignal, orderSignal).map((index, order) => index == order.size - 1)

          L.li(
            toElement(itemID, data, dataSignal, icon),
            L.draggable <-- draggableSignal,
            L.cls(Styles.dragged) <-- Signal.combine(dragTracker.signal, indexSignal).map((dragging, index) =>
              dragging.exists(_.from == index)
            ),
            L.cls(Styles.dropBefore) <-- Signal.combine(dragTracker.signal, indexSignal).map((dragging, index) =>
              dragging.flatMap(_.visibleSlot).contains(index)
            ),
            L.cls(Styles.dropAfter) <-- Signal.combine(dragTracker.signal, indexSignal, isLast).map((dragging, index, last) =>
              last && dragging.flatMap(_.visibleSlot).contains(index + 1)
            ),
            onDragStart(eventFormat, indexSignal, dragTracker.writer),
            onDragOver(dragTracker, indexSignal),
            onDragEnd(dragTracker, orderSignal, orderObserver)
          )
        }

    L.ol(
      L.cls(Styles.list),
      L.children <-- children,
      L.onDrop --> (_.preventDefault()),
      // Leaving the list means there's nowhere to drop
      L.inContext(ctx =>
        L.onDragLeave.filter(event =>
          !event.relatedTarget.isInstanceOf[Node] || !ctx.ref.contains(event.relatedTarget.asInstanceOf[Node])
        ) --> (_ => dragTracker.update(_.map(_.copy(slot = None))))
      )
    )
  }

  @js.native @JSImport("/styles/common/dragSortableList.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val list: String = js.native
    val icon: String = js.native
    val dragged: String = js.native
    val dropBefore: String = js.native
    val dropAfter: String = js.native
  }

  /** @param from the dragged item's position
    * @param slot the gap the item would be dropped into: 0 is before the first item, and the
    *             list's size is after the last
    */
  private final case class Dragging(from: Int, slot: Option[Int]) {
    /** The slot, unless dropping there would leave the item where it is */
    def visibleSlot: Option[Int] =
      slot.filterNot(s => s == from || s == from + 1)
  }

  /** The handle an item is dragged by. Pressing it makes the item draggable until the button is
    * released. Browsers only start a drag once the pointer has moved a few pixels, by which time
    * it can have left the handle, so the item stays draggable when the pointer leaves. */
  private def dragIcon: (L.SvgElement, Signal[Boolean]) = {
    val pressed = Var(false)
    val icon = FontAwesome.icon(FreeSolid.faGripVertical).amend(
      L.svg.cls(Styles.icon),
      L.onMouseDown.mapToStrict(true) --> pressed,
      // A drag that starts ends with dragend instead of mouseup
      L.documentEvents(_.onMouseUp).mapToStrict(false) --> pressed,
      L.documentEvents(_.onDragEnd).mapToStrict(false) --> pressed
    )
    (icon, pressed.signal)
  }

  private def onDragStart(
    eventFormat: String,
    itemIndex: Signal[Int],
    dragTracker: Observer[Option[Dragging]]
  ): L.Modifier[L.HtmlElement] =
    L.inContext(ctx =>
      L.onDragStart.compose(
        // Can't use preventDefault here, since it stops the browser from
        // actually dragging the element
        _.filter(_.target == ctx.ref).withCurrentValueOf(itemIndex)
      ) -->
        dragTracker.contramap[(DragEvent, Int)] { (event, index) =>
          // We don't use this, but it informs other apps not to receive the drop
          event.dataTransfer.setData(eventFormat, "placeholder")
          event.dataTransfer.effectAllowed = DataTransferEffectAllowedKind.move
          Some(Dragging(from = index, slot = None))
        }
    )

  /** Picks the gap above or below the item, depending on which half the pointer is over.
    *
    * Both dragenter and dragover must be cancelled to allow the drop. Dragenter fires each time
    * the pointer crosses into one of the item's children, and the browser shows the no-drop
    * cursor until the next dragover if it isn't cancelled. */
  private def onDragOver(
    dragTracker: Var[Option[Dragging]],
    indexSignal: Signal[Int]
  ): L.Modifier[L.HtmlElement] =
    L.inContext { ctx =>
      val observer = Observer[(DragEvent, Int)] { (event, index) =>
        dragTracker.now().foreach { dragging =>
          event.preventDefault()
          event.dataTransfer.dropEffect = DataTransferDropEffectKind.move
          val bounds = ctx.ref.getBoundingClientRect()
          val slot = if (event.clientY < bounds.top + bounds.height / 2) index else index + 1
          dragTracker.set(Some(dragging.copy(slot = Some(slot))))
        }
      }
      List(
        L.onDragEnter.compose(_.withCurrentValueOf(indexSignal)) --> observer,
        L.onDragOver.compose(_.withCurrentValueOf(indexSignal)) --> observer
      )
    }

  /** Report the new order if the item was dropped somewhere that moves it */
  private def onDragEnd[T](
    dragTracker: Var[Option[Dragging]],
    orderSignal: Signal[List[T]],
    orderObserver: Observer[List[T]]
  ): L.Modifier[L.HtmlElement] = {
    val observer = Observer.combine(
      orderObserver.contracollect[(DragEvent, Option[Dragging], List[T])] {
        case (event, Some(dragging @ Dragging(from, _)), order)
          if event.dataTransfer.dropEffect != DataTransferDropEffectKind.none &&
            dragging.visibleSlot.nonEmpty =>
          move(order, from, dragging.visibleSlot.get)
      },
      dragTracker.writer.contramap[Any](_ => None)
    )

    L.inContext(ctx =>
      L.onDragEnd
        .filterByTarget(_ == ctx.ref)
        .handledWith(_.withCurrentValueOf(dragTracker.signal, orderSignal)) --> observer
    )
  }

  /** Moves the item at `from` into the gap `slot`, counted before the item is taken out */
  private def move[T](order: List[T], from: Int, slot: Int): List[T] = {
    val buffer = order.toBuffer
    val data = buffer.remove(from)
    buffer.insert(if (slot > from) slot - 1 else slot, data)
    buffer.toList
  }
}
