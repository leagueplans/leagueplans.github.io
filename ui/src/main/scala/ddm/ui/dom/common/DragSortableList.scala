package ddm.ui.dom.common

import com.raquo.airstream.core.{EventStream, Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, eventPropToProcessor, seqToModifier}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import ddm.ui.facades.fontawesome.freesolid.FreeSolid
import ddm.ui.utils.airstream.ObservableOps.RichObserverTuple
import ddm.ui.utils.laminar.LaminarOps.RichL
import org.scalajs.dom.html.OList
import org.scalajs.dom.{DragEvent, Event}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object DragSortableList {
  def apply[ID, T](
    id: String,
    orderSignal: Signal[List[T]],
    orderObserver: Observer[List[T]],
    toID: T => ID,
    toElement: (ID, T, Signal[T], L.Div) => L.Children
  ): ReactiveHtmlElement[OList] = {
    val eventFormat = s"application/listitem;id=$id"
    val dragTracker = Var[Option[Dragging[ID, T]]](None)

    val children =
      orderSignal
        .map(_.zipWithIndex)
        .split { case (data, _) => toID(data) } { case (itemID, (data, _), zippedSignal) =>
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

  private def dragIcon: (L.Div, Signal[Boolean]) = {
    val mouseOver = Var(false)
    val icon = L.div(
      L.cls(Styles.icon),
      L.icon(FreeSolid.faGripVertical),
      L.onMouseOver --> mouseOver.writer.contramap[Event](_ => true),
      L.onMouseLeave --> mouseOver.writer.contramap[Event](_ => false)
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
      L.composeEvents(L.onDragStart)(
        // Can't use preventDefault here, since it stops the browser from
        // actually dragging the element
        _.filter(_.target == ctx.ref)
          .withCurrentValueOf(itemIndex)
          .withCurrentValueOf(order)
      ) -->
        dragTracker.contramap[(DragEvent, Int, List[T])] { case (event, originalIndex, originalOrder) =>
          // We don't use this, but it informs other apps not to receive the drop
          event.dataTransfer.setData(eventFormat, "placeholder")
          event.dataTransfer.effectAllowed = "move"
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
      _.withCurrentValueOf(Signal.combine(dragTracker, indexSignal))
        .collect(Function.unlift {
          case (event, Some(dragging), index) =>
            event.preventDefault()
            Option.when(dragging.id != itemID)((dragging, index))
          case _ =>
            None
        })

    val orderMutator =
      orderObserver.contramap[(Dragging[ID, T], Int)] { case (dragging, index) =>
        move(
          dragging.originalOrder,
          from = dragging.originalIndex,
          to = index
        )
      }

    List(
      L.ifUnhandledF(L.onDragEnter)(streamMutator) --> orderMutator,
      L.ifUnhandledF(L.onDragOver)(streamMutator) --> orderMutator
    )
  }

  /** Reset the order to the original order if not dropped successfully */
  private def onDragEnd[ID, T](
    dragTracker: Var[Option[Dragging[ID, T]]],
    orderObserver: Observer[List[T]]
  ): L.Modifier[L.HtmlElement] =
    L.inContext(ctx =>
      L.ifUnhandledF(L.onDragEnd)(
        _.collect { case event if event.target == ctx.ref =>
          event.preventDefault()
          event
        }.withCurrentValueOf(dragTracker)
      ) --> Observer.combine(
        dragTracker.writer.contramap[Any](_ => None),
        orderObserver.contracollect[(DragEvent, Option[Dragging[ID, T]])](
          Function.unlift {
            case (event, Some(dragging)) if event.dataTransfer.dropEffect == "none" =>
              Some(dragging.originalOrder)
            case _ =>
              None
          }
        )
      )
    )

  private def move[T](order: List[T], from: Int, to: Int): List[T] = {
    val buffer = order.toBuffer
    val data = buffer.remove(from)
    buffer.insert(to, data)
    buffer.toList
  }
}
